package org.smartregister.chw.core.worker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.smartregister.AllConstants;
import org.smartregister.chw.core.sync.intent.CloseExpiredReferralsIntentService;
import org.smartregister.job.BaseWorker;

import timber.log.Timber;

/**
 * Created by cozej4 on 2025-02-09.
 *
 * @author cozej4 https://github.com/cozej4
 */
public class CloseExpiredReferralsServiceWorker extends BaseWorker {

    public static final String TAG = "CloseExpiredReferralsServiceJob";

    public CloseExpiredReferralsServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected JobResult onRunJob(@NonNull Params params) {
        // Create an intent to start the IndicatorGeneratorIntentService
        Intent intent = new Intent(getApplicationContext(), CloseExpiredReferralsIntentService.class);

        // Create a PendingIntent with appropriate flags
        PendingIntent pendingIntent = createPendingIntent(getApplicationContext(), intent);

        // Execute the pending intent using the BaseWorker helper method
        boolean executed = executePendingIntent(pendingIntent);
        if (executed) {
            Timber.d("IndicatorGeneratorIntentService started successfully via PendingIntent.");
        } else {
            Timber.e("Failed to execute PendingIntent for IndicatorGeneratorIntentService.");
        }

        // Determine if the job should be rescheduled based on the extras.
        boolean toReschedule = params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false);
        Timber.d("ReportIndicatorGeneratingJob executed; toReschedule = %s", toReschedule);
        return toReschedule ? JobResult.RESCHEDULE : JobResult.SUCCESS;
    }

    /**
     * Helper method to create a PendingIntent for starting a service.
     *
     * @param context the application context.
     * @param intent  the Intent used to start the service.
     * @return a PendingIntent configured with the correct flags.
     */
    private PendingIntent createPendingIntent(Context context, Intent intent) {
        int flags = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(context, 0, intent, flags);
    }
}
