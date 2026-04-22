package org.smartregister.chw.core.activity;

import android.app.Activity;
import android.content.Intent;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.gbv.activity.BaseGbvProfileActivity;
import org.smartregister.chw.core.R;
import org.smartregister.chw.sbc.util.Constants;

public class CoreGbvMemberProfileActivity extends BaseGbvProfileActivity {
    protected RecyclerView notificationAndReferralRecyclerView;
    protected RelativeLayout notificationAndReferralLayout;

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, CoreGbvMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        initializeNotificationReferralRecyclerView();
    }

    protected void initializeNotificationReferralRecyclerView() {
        notificationAndReferralLayout = findViewById(R.id.notification_and_referral_row);
        notificationAndReferralRecyclerView = findViewById(R.id.notification_and_referral_recycler_view);
        if (notificationAndReferralRecyclerView != null) {
            notificationAndReferralRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }
}
