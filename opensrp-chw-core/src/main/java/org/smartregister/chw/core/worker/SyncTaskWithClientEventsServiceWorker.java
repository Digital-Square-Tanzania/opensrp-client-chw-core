package org.smartregister.chw.core.worker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.smartregister.AllConstants;
import org.smartregister.chw.core.sync.intent.SyncClientEventsPerTaskIntentService;
import org.smartregister.job.BaseWorker;
import org.smartregister.sync.intent.SyncIntentService;

import timber.log.Timber;

/**
 * Created by cozej4 on 2025-02-09.
 *
 * @author cozej4 https://github.com/cozej4
 */
public class SyncTaskWithClientEventsServiceWorker extends BaseWorker {

    public static final String TAG = "SyncTaskWithClientEventsServiceJob";

    public SyncTaskWithClientEventsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected JobResult onRunJob(@NonNull Params params) {
        String serviceClassName = getInputData().getString("serviceClassName");

        if (serviceClassName == null) {
            Timber.e("Service class name is null");
            return JobResult.FAILURE;
        }

        try {
            // Convert class name back to a Class<?> object
            Class<?> serviceClass = Class.forName(serviceClassName);
            if (!SyncIntentService.class.isAssignableFrom(serviceClass)) {
                Timber.e("Provided class is not a valid SyncIntentService subclass");
                return JobResult.FAILURE;
            }

            // Create an intent to start the SyncTaskWithClientEventsService
            Intent intent = new Intent(getApplicationContext(), serviceClass);

            // Create a PendingIntent with appropriate flags
            PendingIntent pendingIntent = createPendingIntent(getApplicationContext(), intent);

            // Execute the pending intent using the BaseWorker helper method
            boolean executed = executePendingIntent(pendingIntent);
            if (executed) {
                Timber.d("SyncTaskWithClientEventsServiceWorker started successfully via PendingIntent.");
            } else {
                Timber.e("Failed to execute PendingIntent for SyncTaskWithClientEventsServiceWorker.");
            }

            // Determine if the job should be rescheduled based on the extras.
            boolean toReschedule = params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false);
            Timber.d("SyncTaskWithClientEventsServiceWorker executed; toReschedule = %s", toReschedule);
            return toReschedule ? JobResult.RESCHEDULE : JobResult.SUCCESS;
        } catch (ClassNotFoundException e) {
            Timber.e(e, "Failed to find service class: %s", serviceClassName);
            return JobResult.FAILURE;
        }
    }

    /**
     * Helper method to create a PendingIntent for starting a service.
     *
     * @param context the application context.
     * @param intent  the Intent used to start the service.
     * @return a PendingIntent configured with the correct flags.
     */
    private PendingIntent createPendingIntent(Context context, Intent intent) {
        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= 31) { // API level 31+
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(context, 0, intent, flags);
    }
}
