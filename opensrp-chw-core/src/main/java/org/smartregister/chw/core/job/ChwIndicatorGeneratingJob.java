package org.smartregister.chw.core.job;

import androidx.annotation.NonNull;

import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.worker.ReportIndicatorGeneratingWorker;
import org.smartregister.job.BaseWorker;
import org.smartregister.reporting.job.RecurringIndicatorGeneratingJob;

/**
 * @deprecated As of release 1.6.3-MOH-SNAPSHOT, replaced by {@link org.smartregister.chw.core.worker.ReportIndicatorGeneratingWorker}.
 * This class previously used the Evernote Job library, which is now deprecated.
 * The new implementation leverages Android's native {@link androidx.work.WorkManager} for better
 * compatibility, reliability, and efficiency in background task scheduling.
 *
 * Please migrate to {@link org.smartregister.chw.core.worker.ReportIndicatorGeneratingWorker} to ensure future compatibility.
 */
@Deprecated
public class ChwIndicatorGeneratingJob extends RecurringIndicatorGeneratingJob {
    public static void scheduleJob(String jobTag, Long start, Long flex) {
        BaseWorker.scheduleJob(
                CoreChwApplication.getInstance().getApplicationContext(),
                jobTag,
                start,
                flex,
                ReportIndicatorGeneratingWorker.class);
    }

    public static void scheduleJobImmediately(String jobTag) {
        BaseWorker.scheduleJobImmediately(
                CoreChwApplication.getInstance().getApplicationContext(),
                jobTag,
                ReportIndicatorGeneratingWorker.class);
    }

    @NonNull
    protected Result onRunJob(@NonNull Params params) {
        return super.onRunJob(params);
    }
}
