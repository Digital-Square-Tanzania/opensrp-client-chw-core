package org.smartregister.chw.core.job;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.smartregister.AllConstants;
import org.smartregister.chw.core.sync.intent.CloseExpiredReferralsIntentService;
import org.smartregister.job.BaseJob;

/**
 * Created by cozej4 on 2020-02-08.
 *
 * @author cozej4 https://github.com/cozej4
 */

/**
 * @deprecated As of release 1.6.3-MOH-SNAPSHOT, replaced by {@link org.smartregister.chw.core.worker.CloseExpiredReferralsServiceWorker}.
 * This class previously used the Evernote Job library, which is now deprecated.
 * The new implementation leverages Android's native {@link androidx.work.WorkManager} for better
 * compatibility, reliability, and efficiency in background task scheduling.
 *
 * Please migrate to {@link org.smartregister.chw.core.worker.CloseExpiredReferralsServiceWorker} to ensure future compatibility.
 */
@Deprecated
public class CloseExpiredReferralsServiceJob extends BaseJob {

    public static final String TAG = "CloseExpiredReferralsServiceJob";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Intent intent = new Intent(getApplicationContext(), CloseExpiredReferralsIntentService.class);
        getApplicationContext().startService(intent);
        return params != null && params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false) ? Result.RESCHEDULE : Result.SUCCESS;
    }
}
