package org.smartregister.chw.core.job;

import androidx.annotation.NonNull;

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
    @NonNull
    protected Result onRunJob(@NonNull Params params) {
        return super.onRunJob(params);
    }
}
