package org.smartregister.chw.core.activity;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.getAutoPopulatedJsonEditFormString;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.CoreHarmReductionProfileContract;
import org.smartregister.chw.core.contract.FamilyOtherMemberProfileExtendedContract;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.dataloader.CoreFamilyMemberDataLoader;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.interactor.CoreHarmReductionProfileInteractor;
import org.smartregister.chw.core.model.CoreAllClientsMemberModel;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.presenter.CoreHarmReductionMemberProfilePresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.harmreduction.activity.BaseHarmReductionSoberHouseProfileActivity;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.AlertStatus;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.model.BaseFamilyProfileModel;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.Date;

import timber.log.Timber;

public abstract class CoreHarmReductionSoberHouseProfileActivity extends BaseHarmReductionSoberHouseProfileActivity implements
        FamilyOtherMemberProfileExtendedContract.View, CoreHarmReductionProfileContract.View, FamilyProfileExtendedContract.PresenterCallBack {

    protected RecyclerView notificationAndReferralRecyclerView;
    protected RelativeLayout notificationAndReferralLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateToolbarTitle(this, R.id.toolbar_title, memberObject.getFamilyName());
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        initializeNotificationReferralRecyclerView();
    }

    protected void initializeNotificationReferralRecyclerView() {
        notificationAndReferralLayout = findViewById(R.id.notification_and_referral_row);
        notificationAndReferralRecyclerView = findViewById(R.id.notification_and_referral_recycler_view);
        if (notificationAndReferralRecyclerView != null) {
            notificationAndReferralRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        profilePresenter = new CoreHarmReductionMemberProfilePresenter(this, new CoreHarmReductionProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }

    @Override
    public void refreshFamilyStatus(AlertStatus status) {
        super.refreshFamilyStatus(status);
        rlFamilyServicesDue.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.kvp_profile_menu, menu);
        menu.findItem(R.id.action_location_info).setVisible(UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_registration) {
            if (UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId())) {
                startFormForEdit(R.string.registration_info,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            } else {
                startFormForEdit(R.string.edit_member_form_title,
                        CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }
            return true;
        } else if (itemId == R.id.action_location_info) {
            JSONObject preFilledForm = getAutoPopulatedJsonEditFormString(
                    CoreConstants.JSON_FORM.getFamilyDetailsRegister(), this,
                    UpdateDetailsUtil.getFamilyRegistrationDetails(memberObject.getFamilyBaseEntityId()), Utils.metadata().familyRegister.updateEventType);
            if (preFilledForm != null)
                UpdateDetailsUtil.startUpdateClientDetailsActivity(preFilledForm, this);
            return true;
        } else if (itemId == R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public abstract CoreFamilyOtherMemberActivityPresenter presenter();

    protected abstract Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass();

    protected abstract void removeMember();

    public void startFormForEdit(Integer title_resource, String formName) {

        JSONObject form = null;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils.clientForEdit(memberObject.getBaseEntityId());

        if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {
            form = CoreJsonFormUtils.getAutoPopulatedJsonEditMemberFormString(
                    (title_resource != null) ? getResources().getString(title_resource) : null,
                    CoreConstants.JSON_FORM.getFamilyMemberRegister(),
                    this, client,
                    Utils.metadata().familyMemberRegister.updateEventType, memberObject.getLastName(), false);
        } else if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {
            form = CoreJsonFormUtils.getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), this, formName, CoreConstants.EventType.UPDATE_ANC_REGISTRATION, getResources().getString(title_resource));
        } else if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;
            CommonPersonObjectClient commonPersonObjectClient = UpdateDetailsUtil.getFamilyRegistrationDetails(memberObject.getFamilyBaseEntityId());
            String uniqueID = commonPersonObjectClient.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);
            boolean isPrimaryCareGiver = commonPersonObjectClient.getCaseId().equalsIgnoreCase(memberObject.getFamilyBaseEntityId());

            NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), memberObject.getBaseEntityId());
            binder.setDataLoader(new CoreFamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString,
                    org.smartregister.chw.core.utils.Utils.metadata().familyMemberRegister.updateEventType, uniqueID));
            form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());

            try {
                if (form != null) {
                    UpdateDetailsUtil.startUpdateClientDetailsActivity(form, this);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        try {
            assert form != null;
            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    public abstract void startHivstRegistration();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {
                    FamilyEventClient familyEventClient =
                            new BaseFamilyProfileModel(memberObject.getFamilyName()).processUpdateMemberRegistration(jsonString, memberObject.getBaseEntityId());
                    new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) profilePresenter);
                }
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyRegister.updateEventType)) {
                    FamilyEventClient familyEventClient = new CoreAllClientsMemberModel().processJsonForm(jsonString, memberObject.getFamilyBaseEntityId());
                    familyEventClient.getEvent().setEntityType(CoreConstants.TABLE_NAME.INDEPENDENT_CLIENT);
                    new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) profilePresenter);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    @Override
    public void setProfileName(@NonNull String s) {
        TextView textView = findViewById(org.smartregister.chw.harmreduction.R.id.textview_name);
        textView.setText(s);
    }

    @Override
    public void setProfileDetailOne(@NonNull String s) {
        TextView textView = findViewById(org.smartregister.chw.harmreduction.R.id.textview_gender);
        textView.setText(s);
    }

    @Override
    public void setProfileDetailTwo(@NonNull String s) {
        TextView textView = findViewById(org.smartregister.chw.harmreduction.R.id.textview_address);
        textView.setText(s);
    }

    @Override
    public Context getContext() {
        return this;
    }

    public CoreHarmReductionProfileContract.Presenter getPresenter() {
        return (CoreHarmReductionProfileContract.Presenter) profilePresenter;
    }
}
