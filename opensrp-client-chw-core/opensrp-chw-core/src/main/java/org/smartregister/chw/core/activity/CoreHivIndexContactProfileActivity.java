package org.smartregister.chw.core.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.CoreIndexContactProfileContract;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.dataloader.CoreFamilyMemberDataLoader;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.interactor.CoreHivIndexContactProfileInteractor;
import org.smartregister.chw.core.presenter.CoreHivIndexContactProfilePresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.hiv.activity.BaseIndexContactProfileActivity;
import org.smartregister.chw.hiv.dao.HivIndexDao;
import org.smartregister.chw.hiv.domain.HivIndexContactObject;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.model.BaseFamilyProfileModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.getAutoPopulatedJsonEditFormString;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.hiv.util.Constants.ActivityPayload.HIV_MEMBER_OBJECT;

public abstract class CoreHivIndexContactProfileActivity extends BaseIndexContactProfileActivity implements FamilyProfileExtendedContract.PresenterCallBack, CoreIndexContactProfileContract.View {
    protected RecyclerView notificationAndReferralRecyclerView;
    protected RelativeLayout notificationAndReferralLayout;

    protected static CommonPersonObjectClient getClientDetailsByBaseEntityID(@NonNull String baseEntityId) {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(baseEntityId);
        final CommonPersonObjectClient client =
                new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        return client;

    }

    protected void initializeNotificationReferralRecyclerView() {
        notificationAndReferralLayout = findViewById(R.id.notification_and_referral_row);
        notificationAndReferralRecyclerView = findViewById(R.id.notification_and_referral_recycler_view);
        notificationAndReferralRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeNotificationReferralRecyclerView();
        updateToolbarTitle(this, R.id.toolbar_title, getHivIndexContactObject().getFamilyName());
    }

    @Override
    public void setupViews() {
        super.setupViews();
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        setHivContactProfilePresenter(new CoreHivIndexContactProfilePresenter(this, new CoreHivIndexContactProfileInteractor(), getHivIndexContactObject()));
        fetchProfileData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_registration) {
            if (UpdateDetailsUtil.isIndependentClient(getHivIndexContactObject().getBaseEntityId())) {
                startFormForEdit(R.string.registration_info,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            } else {
                startFormForEdit(R.string.edit_member_form_title,
                        CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }
        } else if (itemId == R.id.action_location_info) {
            JSONObject preFilledForm = getAutoPopulatedJsonEditFormString(
                    CoreConstants.JSON_FORM.getFamilyDetailsRegister(), this,
                    UpdateDetailsUtil.getFamilyRegistrationDetails(getHivIndexContactObject().getFamilyBaseEntityId()), Utils.metadata().familyRegister.updateEventType);
            if (preFilledForm != null)
                UpdateDetailsUtil.startUpdateClientDetailsActivity(preFilledForm, this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hiv_profile_menu, menu);
        menu.findItem(R.id.action_location_info).setVisible(UpdateDetailsUtil.isIndependentClient(getHivIndexContactObject().getBaseEntityId()));
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case JsonFormUtils.REQUEST_CODE_GET_JSON:
                if (resultCode == RESULT_OK) {
                    try {
                        String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                        JSONObject form = new JSONObject(jsonString);
                        if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {
                            FamilyEventClient familyEventClient =
                                    new BaseFamilyProfileModel(getHivIndexContactObject().getFamilyName()).processUpdateMemberRegistration(jsonString, getHivIndexContactObject().getBaseEntityId());
                            new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) getHivContactProfilePresenter());
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                break;
            default:
                break;
        }
    }

    public void onMemberDetailsReloaded(HivIndexContactObject hivIndexContactObject) {
        super.onMemberDetailsReloaded(hivIndexContactObject);
    }

    protected abstract void removeMember();

    public void startFormForEdit(Integer titleResource, String formName) {

        JSONObject form = null;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils.clientForEdit(getHivIndexContactObject().getBaseEntityId());

        if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {
            form = CoreJsonFormUtils.getAutoPopulatedJsonEditMemberFormString(
                    (titleResource != null) ? getResources().getString(titleResource) : null,
                    CoreConstants.JSON_FORM.getFamilyMemberRegister(),
                    this, client,
                    Utils.metadata().familyMemberRegister.updateEventType, getHivIndexContactObject().getLastName(), false);
        } else if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {
            form = CoreJsonFormUtils.getAutoJsonEditAncFormString(
                    getHivIndexContactObject().getBaseEntityId(), this, formName, org.smartregister.chw.hiv.util.Constants.EventType.REGISTRATION, getResources().getString(titleResource));
        } else if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
            String titleString = titleResource != null ? getResources().getString(titleResource) : null;
            CommonPersonObjectClient commonPersonObjectClient = UpdateDetailsUtil.getFamilyRegistrationDetails(getHivIndexContactObject().getFamilyBaseEntityId());
            String uniqueID = commonPersonObjectClient.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);

            NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), getHivIndexContactObject().getBaseEntityId());
            binder.setDataLoader(new CoreFamilyMemberDataLoader(getHivIndexContactObject().getFamilyName(), false, titleString,
                    org.smartregister.chw.core.utils.Utils.metadata().familyMemberRegister.updateEventType, uniqueID));
            JSONObject jsonObject = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());

            try {
                if (jsonObject != null) {
                    UpdateDetailsUtil.startUpdateClientDetailsActivity(jsonObject, this);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        try {
            assert form != null;
            startFormActivity(form, getHivIndexContactObject(), titleResource != null ? getResources().getString(titleResource) : null);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void startFormActivity(JSONObject formJson, HivIndexContactObject hivIndexContactObject, String formName) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, formJson.toString());
        intent.putExtra(HIV_MEMBER_OBJECT, hivIndexContactObject);

        Form form = new Form();
        form.setName(formName);
        form.setActionBarBackground(R.color.family_actionbar);
        form.setNavigationBackground(R.color.family_navigation);
        form.setHomeAsUpIndicator(R.mipmap.ic_cross_white);
        form.setPreviousLabel(getResources().getString(R.string.back));
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);

    }

    private void updateFollowUpVisitButton(String buttonStatus) {
        switch (buttonStatus) {
            case CoreConstants.VISIT_STATE.DUE:
                setFollowUpButtonDue();
                break;
            case CoreConstants.VISIT_STATE.OVERDUE:
                setFollowUpButtonOverdue();
                break;
            default:
                break;
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void checkFollowupStatus() {
        new SetFollowupVisitStatus(getHivIndexContactObject()).execute();
    }


    private class SetFollowupVisitStatus extends AsyncTask<Void, Void, HivIndexContactObject> {
        private HivIndexContactObject hivIndexContactObject;

        public SetFollowupVisitStatus(HivIndexContactObject hivIndexContactObject) {
            this.hivIndexContactObject = hivIndexContactObject;
        }

        @Override
        protected HivIndexContactObject doInBackground(Void... voids) {
            //Refreshing index contacts data from db. these might have been updated once the followup is done
            return HivIndexDao.getMember(hivIndexContactObject.getBaseEntityId());
        }

        @Override
        protected void onPostExecute(HivIndexContactObject indexContactObject) {
            //Updating the HivIndexContact object with the newly queried object
            setHivIndexContactObject(indexContactObject);

            setupFollowupVisitEditViews(!indexContactObject.getHasTheContactClientBeenTested().equals("") && !indexContactObject.getCtcNumber().equals(""));
        }
    }

}
