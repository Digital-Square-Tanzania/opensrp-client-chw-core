package org.smartregister.chw.core.activity;

import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;
import static org.smartregister.chw.fp.util.FamilyPlanningConstants.EVENT_TYPE.FP_FOLLOW_UP_VISIT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jeasy.rules.api.Rules;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.CoreFamilyPlanningMemberProfileContract;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.interactor.CoreFamilyPlanningProfileInteractor;
import org.smartregister.chw.core.presenter.CoreFamilyPlanningProfilePresenter;
import org.smartregister.chw.core.rule.FpAlertRule;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.FpUtil;
import org.smartregister.chw.core.utils.HomeVisitUtil;
import org.smartregister.chw.core.utils.MalariaFollowUpStatusTaskUtil;
import org.smartregister.chw.fp.activity.BaseFpProfileActivity;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.fp.domain.FpMemberObject;
import org.smartregister.chw.fp.domain.Visit;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.model.BaseFamilyProfileModel;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public abstract class CoreFamilyPlanningMemberProfileActivity extends BaseFpProfileActivity implements FamilyProfileExtendedContract.PresenterCallBack, CoreFamilyPlanningMemberProfileContract.View {

    protected RecyclerView notificationAndReferralRecyclerView;
    protected RelativeLayout notificationAndReferralLayout;

    protected static CommonPersonObjectClient getClientDetailsByBaseEntityID(@NonNull String baseEntityId) {
        return org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient(baseEntityId);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        updateToolbarTitle(this, R.id.toolbar_title, fpMemberObject.getFamilyName());
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
    public void setupViews() {
        super.setupViews();
        new UpdateFollowUpVisitButtonTask(fpMemberObject).execute();
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        fpProfilePresenter = new CoreFamilyPlanningProfilePresenter(this, new CoreFamilyPlanningProfileInteractor(), fpMemberObject);
        fetchProfileData();
        fpProfilePresenter.refreshProfileBottom();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_registration) {
            startFormForEdit(R.string.registration_info, CoreConstants.JSON_FORM.FAMILY_MEMBER_REGISTER);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.family_planning_member_profile_menu, menu);
        if (MalariaDao.isRegisteredForMalaria(fpMemberObject.getBaseEntityId())) {
            org.smartregister.util.Utils.startAsyncTask(new MalariaFollowUpStatusTaskUtil(menu, fpMemberObject.getBaseEntityId()), null);
        } else {
            menu.findItem(R.id.action_malaria_registration).setVisible(true);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CoreConstants.ProfileActivityResults.CHANGE_COMPLETED:
                if (resultCode == Activity.RESULT_OK) {
                    Intent intent = new Intent(this, CoreFpRegisterActivity.class);
                    intent.putExtras(getIntent().getExtras());
                    startActivity(intent);
                    finish();
                }
                break;
            case JsonFormUtils.REQUEST_CODE_GET_JSON:
                if (resultCode == RESULT_OK) {
                    try {
                        String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                        JSONObject form = new JSONObject(jsonString);
                        if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {
                            FamilyEventClient familyEventClient = new BaseFamilyProfileModel(fpMemberObject.getFamilyName()).processUpdateMemberRegistration(jsonString, fpMemberObject.getBaseEntityId());
                            new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, (FamilyProfileContract.InteractorCallBack) fpProfilePresenter);
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                break;
            case Constants.REQUEST_CODE_HOME_VISIT:
                refreshViewOnHomeVisitResult();
                break;
            default:
                break;
        }
    }

    private void refreshViewOnHomeVisitResult() {
        Observable<Visit> observable = Observable.create(visitObservableEmitter -> {
            Visit lastVisit = FpDao.getLatestVisit(fpMemberObject.getBaseEntityId(), FP_FOLLOW_UP_VISIT);
            visitObservableEmitter.onNext(lastVisit);
            visitObservableEmitter.onComplete();
        });

        final Disposable[] disposable = new Disposable[1];
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Visit>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onNext(Visit visit) {
                //onMemberDetailsReloaded(fpMemberObject);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                disposable[0].dispose();
                disposable[0] = null;
            }
        });
    }

    protected abstract void removeMember();

    protected abstract void startFamilyPlanningRegistrationActivity();

    public void startFormForEdit(Integer titleResource, String formName) {

        JSONObject form = null;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils.clientForEdit(fpMemberObject.getBaseEntityId());

        if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {
            form = CoreJsonFormUtils.getAutoPopulatedJsonEditMemberFormString((titleResource != null) ? getResources().getString(titleResource) : null, CoreConstants.JSON_FORM.getFamilyMemberRegister(), this, client, Utils.metadata().familyMemberRegister.updateEventType, fpMemberObject.getLastName(), false);
        } else if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {
            form = CoreJsonFormUtils.getAutoJsonEditAncFormString(fpMemberObject.getBaseEntityId(), this, formName, FamilyPlanningConstants.EVENT_TYPE.FP_REGISTRATION, getResources().getString(titleResource));
        }

        try {
            assert form != null;
            startFormActivity(form, fpMemberObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void startFormActivity(JSONObject formJson, FpMemberObject fpMemberObject) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, formJson.toString());
        intent.putExtra(FamilyPlanningConstants.ACTIVITY_PAYLOAD.MEMBER_OBJECT, fpMemberObject);
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

    private class UpdateFollowUpVisitButtonTask extends AsyncTask<Void, Void, Void> {
        private FpMemberObject fpMemberObject;
        private FpAlertRule fpAlertRule;
        private Visit lastVisit;

        public UpdateFollowUpVisitButtonTask(FpMemberObject fpMemberObject) {
            this.fpMemberObject = fpMemberObject;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            lastVisit = FpDao.getLatestVisit(fpMemberObject.getBaseEntityId(), FP_FOLLOW_UP_VISIT);
            Date lastVisitDate = lastVisit != null ? lastVisit.getDate() : null;

            Rules rule = FpUtil.getFpRules(fpMemberObject.getFpMethod());

            fpAlertRule = HomeVisitUtil.getFpVisitStatus(rule, lastVisitDate, FpUtil.parseFpStartDate(fpMemberObject.getFpNextAppointmentDate()), 0, fpMemberObject.getFpMethod());
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            if (fpAlertRule != null && (fpAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE) || fpAlertRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.DUE))) {
                updateFollowUpVisitButton(fpAlertRule.getButtonStatus());
            }
        }
    }
}
