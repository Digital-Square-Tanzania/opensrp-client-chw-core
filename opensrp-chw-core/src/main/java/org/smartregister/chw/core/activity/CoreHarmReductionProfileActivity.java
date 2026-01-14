package org.smartregister.chw.core.activity;

import static org.smartregister.chw.core.utils.Utils.updateToolbarTitle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import org.smartregister.chw.core.interactor.CoreHarmReductionProfileInteractor;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.presenter.CoreHarmReductionMemberProfilePresenter;
import org.smartregister.chw.harmreduction.activity.BaseHarmReductionProfileActivity;
import org.smartregister.domain.AlertStatus;
import org.smartregister.family.util.JsonFormUtils;

import java.util.Date;

public abstract class CoreHarmReductionProfileActivity extends BaseHarmReductionProfileActivity implements
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

    @NonNull
    @Override
    public abstract CoreFamilyOtherMemberActivityPresenter presenter();

    protected abstract Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass();

    protected abstract void removeMember();

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
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
