package org.smartregister.chw.core.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.smartregister.AllConstants;
import org.smartregister.job.BaseWorker;
import org.smartregister.reporting.ReportingLibrary;
import org.smartregister.reporting.dao.ReportIndicatorDaoImpl;
import org.smartregister.reporting.domain.TallyStatus;
import org.smartregister.reporting.event.EventBusHelper;
import org.smartregister.reporting.event.IndicatorTallyEvent;
import org.smartregister.reporting.repository.DailyIndicatorCountRepository;
import org.smartregister.reporting.repository.IndicatorQueryRepository;
import org.smartregister.reporting.repository.IndicatorRepository;
import org.smartregister.repository.AllSharedPreferences;

import timber.log.Timber;

/**
 * Created by cozej4 on 2025-02-09.
 *
 * @author cozej4 https://github.com/cozej4
 */
public class ReportIndicatorGeneratingWorker extends BaseWorker {
    public static final String TAG = "report_indicator_generating_job";

    public ReportIndicatorGeneratingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected JobResult onRunJob(@NonNull Params params) {
        generateIndicatorTallies();

        // Determine if the job should be rescheduled based on the extras.
        boolean toReschedule = params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false);
        Timber.d("ReportIndicatorGeneratingJob executed; toReschedule = %s", toReschedule);
        return toReschedule ? JobResult.RESCHEDULE : JobResult.SUCCESS;
    }

    private void generateIndicatorTallies() {
        Timber.i("ReportIndicatorGeneratingWorker running");

        ReportingLibrary reportingLibrary = ReportingLibrary.getInstance();
        AllSharedPreferences allSharedPreferences = reportingLibrary.getContext().allSharedPreferences();
        String lastProcessedDate = allSharedPreferences.getPreference(ReportIndicatorDaoImpl.REPORT_LAST_PROCESSED_DATE);
        Timber.d("LastProcessedDate %s", lastProcessedDate);

        EventBusHelper.postEvent(new IndicatorTallyEvent(TallyStatus.STARTED));

        IndicatorTallyEvent inProgressTallyEvent = new IndicatorTallyEvent(TallyStatus.INPROGRESS);
        EventBusHelper.postStickyEvent(inProgressTallyEvent);

        boolean generated = false;
        try {
            IndicatorQueryRepository indicatorQueryRepository = reportingLibrary.indicatorQueryRepository();
            DailyIndicatorCountRepository dailyIndicatorCountRepository = reportingLibrary.dailyIndicatorCountRepository();
            IndicatorRepository indicatorRepository = reportingLibrary.indicatorRepository();
            ReportIndicatorDaoImpl reportIndicatorDao = new ReportIndicatorDaoImpl(
                    indicatorQueryRepository,
                    dailyIndicatorCountRepository,
                    indicatorRepository);

            reportIndicatorDao.generateDailyIndicatorTallies(lastProcessedDate);
            generated = true;
        } finally {
            EventBusHelper.removeStickyEvent(inProgressTallyEvent);
        }

        if (generated) {
            EventBusHelper.postEvent(new IndicatorTallyEvent(TallyStatus.COMPLETE));
        }
    }
}
