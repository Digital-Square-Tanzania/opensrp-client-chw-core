package org.smartregister.chw.core.activity;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.getAutoPopulatedJsonEditFormString;
import static org.smartregister.chw.core.utils.UpdateDetailsUtil.getFamilyBaseEntityId;
import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.CoreHivstProfileContract;
import org.smartregister.chw.core.contract.FamilyOtherMemberProfileExtendedContract;
import org.smartregister.chw.core.contract.FamilyProfileExtendedContract;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.ChildDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.dataloader.CoreFamilyMemberDataLoader;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.interactor.CoreHivstProfileInteractor;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.presenter.CoreHivstMemberProfilePresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.hivst.activity.BaseHivstProfileActivity;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public abstract class CoreHivstProfileActivity extends BaseHivstProfileActivity implements FamilyOtherMemberProfileExtendedContract.View, CoreHivstProfileContract.View, FamilyProfileExtendedContract.PresenterCallBack {
    protected static final String OPEN_ISSUE_SELF_TESTING_KITS_FORM = "OPEN_ISSUE_SELF_TESTING_KITS_FORM";
    private CoreHivstProfileActivity.OnMemberTypeLoadedListener onMemberTypeLoadedListener;
    protected RecyclerView notificationAndReferralRecyclerView;
    protected RelativeLayout notificationAndReferralLayout;

    public interface OnMemberTypeLoadedListener {
        void onMemberTypeLoaded(CoreHivstProfileActivity.MemberType memberType);
    }

    protected void initializeNotificationReferralRecyclerView() {
        notificationAndReferralLayout = findViewById(R.id.notification_and_referral_row);
        notificationAndReferralRecyclerView = findViewById(R.id.notification_and_referral_recycler_view);
        notificationAndReferralRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public static class MemberType {

        private final org.smartregister.chw.anc.domain.MemberObject memberObject;
        private final String memberType;

        private MemberType(org.smartregister.chw.anc.domain.MemberObject memberObject, String memberType) {
            this.memberObject = memberObject;
            this.memberType = memberType;
        }

        public org.smartregister.chw.anc.domain.MemberObject getMemberObject() {
            return memberObject;
        }

        public String getMemberType() {
            return memberType;
        }
    }

    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeNotificationReferralRecyclerView();
        updateToolbarTitle(this, R.id.toolbar_title, memberObject.getFamilyName());

        boolean openIssueKitsForm = getIntent().getBooleanExtra(OPEN_ISSUE_SELF_TESTING_KITS_FORM, false);
        if (openIssueKitsForm) {
            startIssueSelfTestingKitsForm(memberObject.getBaseEntityId());
        }
    }

    @Override
    protected void setupViews() {
        super.setupViews();
    }


    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        profilePresenter = new CoreHivstMemberProfilePresenter(this, new CoreHivstProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
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
                    UpdateDetailsUtil.getFamilyRegistrationDetails(getFamilyBaseEntityId(getCommonPersonObjectClient(memberObject.getBaseEntityId()))), Utils.metadata().familyRegister.updateEventType);
            if (preFilledForm != null)
                UpdateDetailsUtil.startUpdateClientDetailsActivity(preFilledForm, this);
            return true;
        } else if (itemId == R.id.action_remove_member) {
            removeMember();
            return true;
        } else if (itemId == R.id.action_cbhs_registration || itemId == R.id.action_hiv_registration) {
            startHivServicesRegistration();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void startHivServicesRegistration();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hivst_profile_menu, menu);
        menu.findItem(R.id.action_location_info).setVisible(UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId()));
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED) {
            Intent intent = new Intent(this, getFamilyProfileActivityClass());
            intent.putExtras(getIntent().getExtras());
            startActivity(intent);
            finish();
        }
    }

    protected static CommonPersonObjectClient getClientDetailsByBaseEntityID(@NonNull String baseEntityId) {
        return getCommonPersonObjectClient(baseEntityId);
    }

    protected abstract Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass();

    protected abstract void removeMember();

    @NonNull
    @Override
    public abstract CoreFamilyOtherMemberActivityPresenter presenter();

    public CoreHivstProfileContract.Presenter getPresenter() {
        return (CoreHivstProfileContract.Presenter) profilePresenter;
    }

    public void setOnMemberTypeLoadedListener(CoreHivstProfileActivity.OnMemberTypeLoadedListener onMemberTypeLoadedListener) {
        this.onMemberTypeLoadedListener = onMemberTypeLoadedListener;
    }

    @Override
    public void setProfileName(@NonNull String s) {
        TextView textView = findViewById(org.smartregister.hivst.R.id.textview_name);
        textView.setText(s);
    }

    @Override
    public void setProfileDetailOne(@NonNull String s) {
        TextView textView = findViewById(org.smartregister.hivst.R.id.textview_gender);
        textView.setText(s);
    }

    @Override
    public void setProfileDetailTwo(@NonNull String s) {
        TextView textView = findViewById(org.smartregister.hivst.R.id.textview_address);
        textView.setText(s);
    }

    public void startFormForEdit(Integer title_resource, String formName) {

        JSONObject form = null;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils.clientForEdit(memberObject.getBaseEntityId());

        if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {
            form = CoreJsonFormUtils.getAutoPopulatedJsonEditMemberFormString(
                    (title_resource != null) ? getResources().getString(title_resource) : null,
                    CoreConstants.JSON_FORM.getFamilyMemberRegister(),
                    this, client,
                    Utils.metadata().familyMemberRegister.updateEventType, memberObject.getLastName(), false);
        } else if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;
            CommonPersonObjectClient commonPersonObjectClient = UpdateDetailsUtil.getFamilyRegistrationDetails(getFamilyBaseEntityId(getCommonPersonObjectClient(memberObject.getBaseEntityId())));
            String uniqueID = commonPersonObjectClient.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);
            boolean isPrimaryCareGiver = commonPersonObjectClient.getCaseId().equalsIgnoreCase(memberObject.getFamilyBaseEntityId());

            NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), memberObject.getBaseEntityId());
            binder.setDataLoader(new CoreFamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString,
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
            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public Context getContext() {
        return this;
    }

    protected void executeOnLoaded(CoreHivstProfileActivity.OnMemberTypeLoadedListener listener) {
        final Disposable[] disposable = new Disposable[1];
        getMemberType().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CoreHivstProfileActivity.MemberType>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable[0] = d;
                    }

                    @Override
                    public void onNext(CoreHivstProfileActivity.MemberType memberType) {
                        listener.onMemberTypeLoaded(memberType);
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

    @Override
    public void openMedicalHistory() {
        executeOnLoaded(onMemberTypeLoadedListener);
    }

    protected Observable<CoreHivstProfileActivity.MemberType> getMemberType() {
        return Observable.create(e -> {
            org.smartregister.chw.anc.domain.MemberObject ancMemberObject = PNCDao.getMember(memberObject.getBaseEntityId());
            String type = null;

            if (AncDao.isANCMember(ancMemberObject.getBaseEntityId())) {
                type = CoreConstants.TABLE_NAME.ANC_MEMBER;
            } else if (PNCDao.isPNCMember(ancMemberObject.getBaseEntityId())) {
                type = CoreConstants.TABLE_NAME.PNC_MEMBER;
            } else if (ChildDao.isChild(ancMemberObject.getBaseEntityId())) {
                type = CoreConstants.TABLE_NAME.CHILD;
            } else {
                type = CoreConstants.TABLE_NAME.PNC_MEMBER;
            }

            CoreHivstProfileActivity.MemberType memberType = new CoreHivstProfileActivity.MemberType(ancMemberObject, type);
            e.onNext(memberType);
            e.onComplete();
        });
    }

}
