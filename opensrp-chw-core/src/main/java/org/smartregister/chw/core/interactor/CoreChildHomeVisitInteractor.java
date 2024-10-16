package org.smartregister.chw.core.interactor;

import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.interactor.BaseAncHomeVisitInteractor;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class CoreChildHomeVisitInteractor extends BaseAncHomeVisitInteractor {

    private Flavor flavor;

    public CoreChildHomeVisitInteractor(Flavor flavor) {
        this.flavor = flavor;
    }

    @Override
    public void calculateActions(final BaseAncHomeVisitContract.View view, final MemberObject memberObject, final BaseAncHomeVisitContract.InteractorCallBack callBack) {
        // update the local database incase of manual date adjustment

        final Runnable runnable = () -> {

            try {
                VisitUtils.processVisits(memberObject.getBaseEntityId());
            } catch (Exception e) {
                Timber.e(e);
            }

            final LinkedHashMap<String, BaseAncHomeVisitAction> actionList = new LinkedHashMap<>();

            try {
                for (Map.Entry<String, BaseAncHomeVisitAction> entry : flavor.calculateActions(view, memberObject, callBack).entrySet()) {
                    actionList.put(entry.getKey(), entry.getValue());
                }
            } catch (BaseAncHomeVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * Injects implementation specific changes to the event
     *
     * @param baseEvent
     */
    @Override
    protected void prepareEvent(Event baseEvent) {
        if (baseEvent != null) {
            // add anc date obs and last
            List<Object> list = new ArrayList<>();
            list.add(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
            baseEvent.addObs(new Obs("concept", "text", "home_visit_date", "",
                    list, new ArrayList<>(), null, "home_visit_date"));

            List<Object> ecd_modules_present = new ArrayList<>();
            ecd_modules_present.add(areEcdModulesPresent(baseEvent));

            baseEvent.addObs(new Obs("concept", "text", "ecd_modules_present", "",
                    ecd_modules_present, ecd_modules_present, null, "ecd_modules_present"));
        }
    }

    private static boolean areEcdModulesPresent(Event baseEvent) {
        for (Obs obs : baseEvent.getObs()) {
            String fieldCode = obs.getFieldCode();
            if (fieldCode != null && (
                    fieldCode.equals("ccd_development_screening_assessment_module") ||
                            fieldCode.equals("child_development_issues") ||
                            fieldCode.equals("ccd_introduction_module") ||
                            fieldCode.equals("ccd_communication_assessment_module") ||
                            fieldCode.equals("ccd_problem_solving_module") ||
                            fieldCode.equals("ccd_caregiver_responsiveness_module") ||
                            fieldCode.equals("ccd_play_assessment_counselling_module"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getEncounterType() {
        return CoreConstants.EventType.CHILD_HOME_VISIT;
    }

    @Override
    protected String getTableName() {
        return CoreConstants.TABLE_NAME.CHILD;
    }

    public interface Flavor {
        LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(final BaseAncHomeVisitContract.View view, MemberObject memberObject, final BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException;
    }
}