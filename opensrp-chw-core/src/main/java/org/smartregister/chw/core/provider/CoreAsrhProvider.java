package org.smartregister.chw.core.provider;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.asrh.dao.AsrhDao;
import org.smartregister.chw.asrh.domain.MemberObject;
import org.smartregister.chw.asrh.provider.BaseAsrhRegisterProvider;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.rule.AsrhFollowupRule;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FpUtil;
import org.smartregister.chw.core.utils.HomeVisitUtil;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class CoreAsrhProvider extends BaseAsrhRegisterProvider {

    private Context context;
    private View.OnClickListener onClickListener;

    public CoreAsrhProvider(Context context, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener) {
        super(context, paginationClickListener, onClickListener, visibleColumns);
        this.context = context;
        this.onClickListener = onClickListener;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);

        viewHolder.dueButton.setVisibility(View.GONE);
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        viewHolder.dueButton.setOnClickListener(null);
        Utils.startAsyncTask(new UpdateAsyncTask(context, viewHolder, pc), null);
    }

    private void updateDueColumn(Context context, RegisterViewHolder viewHolder, AsrhFollowupRule asrhFollowupRule, String nextAppointmentDate) {
        if (asrhFollowupRule.getDueDate() != null) {
            viewHolder.dueButton.setVisibility(View.VISIBLE);
            if (asrhFollowupRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.NOT_DUE_YET)) {
                setVisitButtonNextDueStatus(context, FpUtil.sdf.format(asrhFollowupRule.getDueDate()), viewHolder.dueButton);
            }
            if (asrhFollowupRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.DUE)) {
                setVisitButtonDueStatus(context, String.valueOf(Days.daysBetween(new DateTime(asrhFollowupRule.getDueDate()), new DateTime()).getDays()), viewHolder.dueButton);
            } else if (asrhFollowupRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.OVERDUE)) {
                setVisitButtonOverdueStatus(context, String.valueOf(Days.daysBetween(new DateTime(asrhFollowupRule.getOverDueDate()), new DateTime()).getDays()), viewHolder.dueButton);
            } else if (asrhFollowupRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.VISIT_DONE)) {
                setVisitDone(context, viewHolder.dueButton);
            }
        } else if (StringUtils.isNotBlank(nextAppointmentDate)) {
            viewHolder.dueButton.setVisibility(View.VISIBLE);
            setVisitButtonNextDueStatus(context, nextAppointmentDate, viewHolder.dueButton);
        }
    }

    private void setVisitButtonNextDueStatus(Context context, String visitDue, Button dueButton) {
        dueButton.setTextColor(context.getResources().getColor(R.color.light_grey_text));
        dueButton.setText(context.getString(R.string.hiv_visit_day_next_due, visitDue));
        dueButton.setBackgroundResource(R.drawable.colorless_btn_selector);
        dueButton.setOnClickListener(null);
    }


    private void setVisitButtonDueStatus(Context context, String visitDue, Button dueButton) {
        dueButton.setTextColor(context.getResources().getColor(R.color.alert_in_progress_blue));
        if (visitDue.equalsIgnoreCase("0")) {
            dueButton.setText(context.getString(R.string.hiv_visit_day_due_today));
        } else {
            dueButton.setText(context.getString(R.string.hiv_visit_day_due, visitDue));
        }
        dueButton.setBackgroundResource(R.drawable.blue_btn_selector);
        dueButton.setOnClickListener(onClickListener);
    }

    private void setVisitButtonOverdueStatus(Context context, String visitDue, Button dueButton) {
        dueButton.setTextColor(context.getResources().getColor(R.color.white));
        if (visitDue.equalsIgnoreCase("0")) {
            dueButton.setText(context.getString(R.string.hiv_visit_day_overdue_today));

        } else {
            dueButton.setText(context.getString(R.string.hiv_visit_day_overdue, visitDue));
        }
        dueButton.setBackgroundResource(R.drawable.overdue_red_btn_selector);
        dueButton.setOnClickListener(onClickListener);
    }

    private void setVisitDone(Context context, Button dueButton) {
        dueButton.setTextColor(context.getResources().getColor(R.color.alert_complete_green));
        dueButton.setText(context.getString(R.string.visit_done));
        dueButton.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        dueButton.setOnClickListener(null);
    }


    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final RegisterViewHolder viewHolder;
        private final CommonPersonObjectClient pc;
        private final Context context;
        private AsrhFollowupRule asrhFollowupRule;
        private MemberObject memberObject;

        private UpdateAsyncTask(Context context, RegisterViewHolder viewHolder, CommonPersonObjectClient pc) {
            this.context = context;
            this.viewHolder = viewHolder;
            this.pc = pc;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String baseEntityID = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
            memberObject = AsrhDao.getMember(baseEntityID);

            Date nextAppointmentDate = null;
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                nextAppointmentDate = simpleDateFormat.parse(memberObject.getNextAppointmentDate());
            } catch (Exception e) {
                Timber.e(e);
            }
            //TODO finalize this implementation
            asrhFollowupRule = HomeVisitUtil.getAsrhVisitStatus(nextAppointmentDate);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            if (asrhFollowupRule != null && !asrhFollowupRule.getButtonStatus().equalsIgnoreCase(CoreConstants.VISIT_STATE.EXPIRED) && memberObject != null) {
                updateDueColumn(context, viewHolder, asrhFollowupRule, memberObject.getNextAppointmentDate());
            }
        }
    }
}