package org.smartregister.chw.core.job;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;

import org.smartregister.AllConstants;
import org.smartregister.chw.core.sync.intent.SyncClientEventsPerTaskIntentService;
import org.smartregister.job.BaseJob;

/**
 * Created by cozej4 on 2020-02-08.
 *
 * @author cozej4 https://github.com/cozej4
 */

/**
 * @deprecated As of release 1.6.3-MOH-SNAPSHOT, replaced by {@link org.smartregister.chw.core.worker.SyncTaskWithClientEventsServiceWorker}.
 * This class previously used the Evernote Job library, which is now deprecated.
 * The new implementation leverages Android's native {@link androidx.work.WorkManager} for better
 * compatibility, reliability, and efficiency in background task scheduling.
 *
 * Please migrate to {@link org.smartregister.chw.core.worker.SyncTaskWithClientEventsServiceWorker} to ensure future compatibility.
 */
@Deprecated
public class SyncTaskWithClientEventsServiceJob extends BaseJob {

    public static final String TAG = "SyncTaskWithClientEventsServiceJob";

    private Class<? extends SyncClientEventsPerTaskIntentService> serviceClass;

    public SyncTaskWithClientEventsServiceJob(Class<? extends SyncClientEventsPerTaskIntentService> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @NonNull
    @Override
    protected Job.Result onRunJob(@NonNull Job.Params params) {
        Intent intent = new Intent(getApplicationContext(), serviceClass);
        getApplicationContext().startService(intent);
        return params != null && params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false) ? Result.RESCHEDULE : Result.SUCCESS;
    }
}
