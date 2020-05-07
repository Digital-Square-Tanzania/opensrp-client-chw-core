package org.smartregister.chw.core.sync.intent;

import android.content.Intent;

import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.ChwDBConstants;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.referral.util.DBConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Task;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.TaskRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

import static org.smartregister.chw.core.utils.ChwDBConstants.TaskTable;
import static org.smartregister.chw.core.utils.CoreConstants.TASKS_FOCUS;

/**
 * Created by cozej4 on 2020-02-08.
 *
 * @author cozej4 https://github.com/cozej4
 */
public class CloseExpiredReferralsIntentService extends ChwCoreSyncIntentService {

    private static final String TAG = CloseExpiredReferralsIntentService.class.getSimpleName();
    private final CommonRepository commonRepository;
    private final TaskRepository taskRepository;
    private AllSharedPreferences sharedPreferences;
    private ECSyncHelper syncHelper;


    public CloseExpiredReferralsIntentService() {
        super(TAG);
        commonRepository = Utils.context().commonrepository("task");
        taskRepository = CoreLibrary.getInstance().context().getTaskRepository();
        sharedPreferences = Utils.getAllSharedPreferences();
        syncHelper = FamilyLibrary.getInstance().getEcSyncHelper();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<CommonPersonObject> tasks = commonRepository.customQuery(
                String.format(
                        "SELECT * FROM %s LEFT JOIN %s ON %s.%s = %s.%s WHERE %s = ?  ORDER BY %s DESC",
                        CoreConstants.TABLE_NAME.TASK, CoreConstants.TABLE_NAME.REFERRAL, CoreConstants.TABLE_NAME.TASK, ChwDBConstants.TaskTable.FOR, CoreConstants.TABLE_NAME.REFERRAL, CommonRepository.ID_COLUMN,
                        ChwDBConstants.TaskTable.BUSINESS_STATUS, TaskTable.START),
                new String[]{CoreConstants.BUSINESS_STATUS.REFERRED}, CoreConstants.TABLE_NAME.TASK);

        for (CommonPersonObject task : tasks) {
            String appointmentDate = task.getColumnmaps().get(DBConstants.Key.REFERRAL_APPOINTMENT_DATE);
            String startDate = task.getColumnmaps().get(TaskTable.START);
            String focus = task.getDetails().get(ChwDBConstants.TaskTable.FOCUS);
            if (focus != null && startDate != null) {
                Calendar expiredCalendar = Calendar.getInstance();
                if (focus.equals(TASKS_FOCUS.ANC_DANGER_SIGNS) || focus.equals(TASKS_FOCUS.PNC_DANGER_SIGNS)) {
                    expiredCalendar.setTimeInMillis(Long.parseLong(startDate));
                    expiredCalendar.add(Calendar.HOUR_OF_DAY, 24);
                    checkIfExpired(expiredCalendar, task);
                } else if (focus.equals(TASKS_FOCUS.SICK_CHILD) || focus.equals(TASKS_FOCUS.SUSPECTED_MALARIA) || focus.equals(TASKS_FOCUS.FP_SIDE_EFFECTS)) {
                    Calendar referralNotYetDoneCalendar = Calendar.getInstance();
                    referralNotYetDoneCalendar.setTimeInMillis(Long.parseLong(startDate));
                    referralNotYetDoneCalendar.add(Calendar.DAY_OF_MONTH, 3);

                    expiredCalendar.setTimeInMillis(Long.parseLong(startDate));
                    expiredCalendar.add(Calendar.DAY_OF_MONTH, 7);

                    if (Objects.requireNonNull(task.getColumnmaps().get(TaskTable.STATUS)).equals(Task.TaskStatus.READY.name())) {
                        checkIfNotYetDone(referralNotYetDoneCalendar, task);
                    } else {
                        checkIfExpired(expiredCalendar, task);
                    }
                } else if (focus.equals(TASKS_FOCUS.SUSPECTED_TB)) {
                    expiredCalendar.setTimeInMillis(Long.parseLong(appointmentDate));
                    expiredCalendar.add(Calendar.DAY_OF_MONTH, 3);

                    checkIfExpired(expiredCalendar, task);
                } else {
                    if (appointmentDate != null && !appointmentDate.isEmpty()) {
                        expiredCalendar.setTimeInMillis(Long.parseLong(appointmentDate));
                    } else {
                        expiredCalendar.setTimeInMillis(Long.parseLong(startDate));
                    }
                    expiredCalendar.add(Calendar.DAY_OF_MONTH, 7);

                    checkIfExpired(expiredCalendar, task);

                }
            }
        }
    }

    public void checkIfExpired(Calendar expiredCalendar, CommonPersonObject taskEvent) {
        if (Calendar.getInstance().getTime().after(expiredCalendar.getTime())) {
            saveExpiredReferralEvent(
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.FOR),
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.LOCATION),
                    taskEvent.getColumnmaps().get(CommonRepository.ID_COLUMN),
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.STATUS),
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.BUSINESS_STATUS)

            );
        }
    }

    public void checkIfNotYetDone(Calendar referralNotYetDoneCalendar, CommonPersonObject taskEvent) {
        if (Calendar.getInstance().getTime().after(referralNotYetDoneCalendar.getTime())) {
            saveNotYetDoneReferralEvent(
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.FOR),
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.LOCATION),
                    taskEvent.getColumnmaps().get(CommonRepository.ID_COLUMN),
                    taskEvent.getColumnmaps().get(ChwDBConstants.TaskTable.STATUS)
            );
        }
    }

    private void saveExpiredReferralEvent(String baseEntityId, String userLocationId, String referralTaskId, String taskStatus, String businessStatus) {
        try {

            Event baseEvent = generateEvent(baseEntityId, userLocationId, referralTaskId, taskStatus);
            baseEvent.setEventType(CoreConstants.EventType.EXPIRED_REFERRAL);
            baseEvent.setEntityType((CoreConstants.TABLE_NAME.CLOSE_REFERRAL));

            baseEvent.addObs((new Obs())
                    .withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS)
                    .withValue(businessStatus)
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_BUSINESS_STATUS)
                    .withFieldType(CoreConstants.FORMSUBMISSION_FIELD).withFieldDataType(CoreConstants.TEXT).withParentCode("")
                    .withHumanReadableValues(new ArrayList<>()));

            CoreJsonFormUtils.tagSyncMetadata(sharedPreferences, baseEvent);

            baseEvent.setLocationId(userLocationId); //setting the location uuid of the referral initiator so that to allow the event to sync back to the chw app since it sync data by location.
            syncEvents(baseEvent, referralTaskId);
        } catch (Exception e) {
            Timber.e(e, "CloseExpiredReferralsIntentService --> saveExpiredReferralEvent");
        }
    }

    private void saveNotYetDoneReferralEvent(String baseEntityId, String userLocationId, String referralTaskId, String taskStatus) {
        try {
            Event baseEvent = generateEvent(baseEntityId, userLocationId, referralTaskId, taskStatus);
            baseEvent.setEventType((CoreConstants.EventType.NOT_YET_DONE_REFERRAL));
            baseEvent.setEntityType((CoreConstants.TABLE_NAME.NOT_YET_DONE_REFERRAL));

            CoreJsonFormUtils.tagSyncMetadata(sharedPreferences, baseEvent);
            baseEvent.setLocationId(userLocationId);  //setting the location uuid of the referral initiator so that to allow the event to sync back to the chw app since it sync data by location.

            syncEvents(baseEvent, referralTaskId);
        } catch (Exception e) {
            Timber.e(e, "CloseExpiredReferralsIntentService --> saveExpiredReferralEvent");
        }
    }

    private void syncEvents(Event baseEvent, String referralTaskId) {
        try {
            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            syncHelper.addEvent(referralTaskId, eventJson);
            long lastSyncTimeStamp = sharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            List<String> formSubmissionIds = new ArrayList<>();
            formSubmissionIds.add(baseEvent.getFormSubmissionId());
            CoreChwApplication.getInstance().getClientProcessorForJava().processClient(syncHelper.getEvents(formSubmissionIds));
            sharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "CloseExpiredReferralsIntentService --> syncEvents");
        }
    }

    private Event generateEvent(String baseEntityId, String userLocationId, String referralTaskId, String taskStatus) {
        Event baseEvent = null;
        try {
            AllSharedPreferences sharedPreferences = org.smartregister.family.util.Utils.getAllSharedPreferences();
            baseEvent = (Event) new Event()
                    .withBaseEntityId(baseEntityId)
                    .withEventDate(new Date())
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withProviderId(sharedPreferences.fetchRegisteredANM())
                    .withLocationId(userLocationId)
                    .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                    .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                    .withDateCreated(new Date());

            baseEvent.addObs((new Obs())
                    .withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK)
                    .withValue(referralTaskId)
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK)
                    .withFieldType(CoreConstants.FORMSUBMISSION_FIELD).withFieldDataType(CoreConstants.TEXT)
                    .withParentCode("").withHumanReadableValues(new ArrayList<>()));

            baseEvent.addObs((new Obs())
                    .withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS)
                    .withValue(taskStatus)
                    .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.REFERRAL_TASK_PREVIOUS_STATUS)
                    .withFieldType(CoreConstants.FORMSUBMISSION_FIELD).withFieldDataType(CoreConstants.TEXT)
                    .withParentCode("")
                    .withHumanReadableValues(new ArrayList<>()));

            CoreJsonFormUtils.tagSyncMetadata(sharedPreferences, baseEvent);
            //setting the location uuid of the referral initiator so that to allow the event to sync back to the chw app since it sync data by location.
            baseEvent.setLocationId(userLocationId);
        } catch (Exception e) {
            Timber.e(e, "CloseExpiredReferralsIntentService --> saveExpiredReferralEvent");
        }
        return baseEvent;
    }

}
