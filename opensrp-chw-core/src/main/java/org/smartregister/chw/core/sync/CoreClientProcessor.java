package org.smartregister.chw.core.sync;

import static org.smartregister.chw.cecap.util.Constants.EVENT_TYPE.CECAP_HEALTH_EDUCATION_MOBILIZATION;
import static org.smartregister.chw.sbc.util.Constants.EVENT_TYPE.SBC_HEALTH_EDUCATION_MOBILIZATION;
import static org.smartregister.chw.sbc.util.Constants.EVENT_TYPE.SBC_MONTHLY_SOCIAL_MEDIA_REPORT;
import static org.smartregister.chw.tbleprosy.util.Constants.EVENT_TYPE.TB_LEPROSY_MOBILIZATION;

import android.content.ContentValues;
import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.cdp.CdpLibrary;
import org.smartregister.chw.cdp.dao.CdpOrderDao;
import org.smartregister.chw.cdp.dao.CdpStockingDao;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.ChildDao;
import org.smartregister.chw.core.dao.ChwNotificationDao;
import org.smartregister.chw.core.dao.CoreHivDao;
import org.smartregister.chw.core.dao.EventDao;
import org.smartregister.chw.core.dao.SbccDao;
import org.smartregister.chw.core.domain.MonthlyTally;
import org.smartregister.chw.core.domain.StockUsage;
import org.smartregister.chw.core.model.CommunityResponderModel;
import org.smartregister.chw.core.repository.CommunityResponderRepository;
import org.smartregister.chw.core.repository.MonthlyTalliesRepository;
import org.smartregister.chw.core.repository.StockUsageReportRepository;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.core.utils.ReportUtils;
import org.smartregister.chw.core.utils.StockUsageReportUtils;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.hivst.dao.HivstMobilizationDao;
import org.smartregister.chw.hps.dao.HpsDao;
import org.smartregister.chw.hts.dao.HtsDao;
import org.smartregister.chw.lab.LabLibrary;
import org.smartregister.chw.lab.dao.LabDao;
import org.smartregister.chw.malaria.util.Constants;
import org.smartregister.chw.malaria.util.MalariaUtil;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.tbleprosy.dao.TbLeprosyMobilizationDao;
import org.smartregister.clientandeventmodel.DateUtil;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.Column;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.service.intent.RecurringIntentService;
import org.smartregister.immunization.service.intent.VaccineIntentService;
import org.smartregister.immunization.util.IMConstants;
import org.smartregister.sync.ClientProcessorForJava;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class CoreClientProcessor extends ClientProcessorForJava {

    private ClientClassification classification;
    private Table vaccineTable;
    private Table serviceTable;
    private Map<String, Table> serviceTables;

    protected CoreClientProcessor(Context context) {
        super(context);
    }

    public static ClientProcessorForJava getInstance(Context context) {
        if (instance == null) {
            instance = new CoreClientProcessor(context);
        }
        return instance;
    }

    public static void addVaccine(VaccineRepository vaccineRepository, Vaccine vaccine) {
        try {
            if (vaccineRepository == null || vaccine == null) {
                return;
            }

            // if its an updated vaccine, delete the previous object
            vaccineRepository.deleteVaccine(vaccine.getBaseEntityId(), vaccine.getName());

            // Add the vaccine
            vaccineRepository.add(vaccine);

            String name = vaccine.getName();
            if (StringUtils.isBlank(name)) {
                return;
            }

            // Update vaccines in the same group where either can be given
            // For example measles 1 / mr 1
            name = VaccineRepository.removeHyphen(name);
            String ftsVaccineName = null;

            if (VaccineRepo.Vaccine.measles1.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.mr1.display();
            } else if (VaccineRepo.Vaccine.mr1.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.measles1.display();
            } else if (VaccineRepo.Vaccine.measles2.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.mr2.display();
            } else if (VaccineRepo.Vaccine.mr2.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.measles2.display();
            }

            if (ftsVaccineName != null) {
                ftsVaccineName = VaccineRepository.addHyphen(ftsVaccineName.toLowerCase());
                Vaccine ftsVaccine = new Vaccine();
                ftsVaccine.setBaseEntityId(vaccine.getBaseEntityId());
                ftsVaccine.setName(ftsVaccineName);
                vaccineRepository.updateFtsSearch(ftsVaccine);
            }

        } catch (Exception e) {
            Timber.e(e);
        }

    }

    @Override
    public synchronized void processClient(List<EventClient> eventClients) throws Exception {

        ClientClassification clientClassification = getClassification();
        Table vaccineTable = getVaccineTable();
        Table serviceTable = getServiceTable();

        if (!eventClients.isEmpty()) {
            for (EventClient eventClient : eventClients) {
                Event event = eventClient.getEvent();
                if (event == null) {
                    return;
                }

                String eventType = event.getEventType();
                if (eventType == null) {
                    continue;
                }

                processEvents(clientClassification, vaccineTable, serviceTable, eventClient, event, eventType);
            }

        }
    }

    private ClientClassification getClassification() {
        if (classification == null) {
            classification = assetJsonToJava("ec_client_classification.json", ClientClassification.class);
        }
        return classification;
    }

    private Table getVaccineTable() {
        if (vaccineTable == null) {
            vaccineTable = assetJsonToJava("ec_client_vaccine.json", Table.class);
        }
        return vaccineTable;
    }

    private Table getServiceTable() {
        if (serviceTables == null) {
            loadServiceTables();
        }
        if (serviceTable != null) {
            return serviceTable;
        }
        if (serviceTables != null && !serviceTables.isEmpty()) {
            serviceTable = serviceTables.values().iterator().next();
        }
        if (serviceTable == null) {
            serviceTable = assetJsonToJava("ec_client_service.json", Table.class);
            if (serviceTable != null) {
                if (serviceTables == null) {
                    serviceTables = new HashMap<>();
                }
                serviceTables.put(serviceTable.name, serviceTable);
            }
        }
        return serviceTable;
    }

    protected Table getServiceTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return getServiceTable();
        }
        if (serviceTables == null) {
            loadServiceTables();
        }
        if (serviceTables != null && serviceTables.containsKey(tableName)) {
            return serviceTables.get(tableName);
        }
        return getServiceTable();
    }

    private void loadServiceTables() {
        Table[] tables = assetJsonToJava("ec_client_service.json", Table[].class);
        if (tables != null && tables.length > 0) {
            serviceTables = new HashMap<>();
            for (Table table : tables) {
                if (table != null && StringUtils.isNotBlank(table.name)) {
                    serviceTables.put(table.name, table);
                }
            }
            if (!serviceTables.isEmpty()) {
                serviceTable = serviceTables.values().iterator().next();
            }
        }
    }

    protected void processEvents(ClientClassification clientClassification, Table vaccineTable, Table serviceTable, EventClient eventClient, Event event, String eventType) throws Exception {
        switch (eventType) {
            case VaccineIntentService.EVENT_TYPE:
            case VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT:
                if (vaccineTable == null) {
                    return;
                }
                processVaccine(eventClient, vaccineTable, eventType.equals(VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT));
                break;
            case RecurringIntentService.EVENT_TYPE:
                if (serviceTable == null) {
                    return;
                }
                processService(eventClient, serviceTable);
                break;
            case CoreConstants.EventType.CHILD_HOME_VISIT:
                processVisitEvent(Utils.processOldEvents(eventClient), CoreConstants.EventType.CHILD_HOME_VISIT);
                processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                break;
            case CoreConstants.EventType.CHILD_VISIT_NOT_DONE:
            case CoreConstants.EventType.WASH_CHECK:
            case CoreConstants.EventType.FAMILY_KIT:
            case CoreConstants.EventType.ROUTINE_HOUSEHOLD_VISIT:
                processVisitEvent(eventClient);
                processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                break;
            case CoreConstants.EventType.MINIMUM_DIETARY_DIVERSITY:
            case CoreConstants.EventType.MUAC:
            case CoreConstants.EventType.LLITN:
            case CoreConstants.EventType.ECD:
            case CoreConstants.EventType.DEWORMING:
            case CoreConstants.EventType.VITAMIN_A:
            case CoreConstants.EventType.EXCLUSIVE_BREASTFEEDING:
            case CoreConstants.EventType.MNP:
            case CoreConstants.EventType.IPTP_SP:
            case CoreConstants.EventType.TT:
            case CoreConstants.EventType.VACCINE_CARD_RECEIVED:
            case CoreConstants.EventType.DANGER_SIGNS_BABY:
            case CoreConstants.EventType.PNC_HEALTH_FACILITY_VISIT:
            case CoreConstants.EventType.KANGAROO_CARE:
            case CoreConstants.EventType.UMBILICAL_CORD_CARE:
            case CoreConstants.EventType.IMMUNIZATION_VISIT:
            case CoreConstants.EventType.OBSERVATIONS_AND_ILLNESS:
            case CoreConstants.EventType.SICK_CHILD:
                processVisitEvent(eventClient, CoreConstants.EventType.CHILD_HOME_VISIT);
                processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                break;
            case CoreConstants.EventType.ANC_HOME_VISIT:
            case org.smartregister.chw.anc.util.Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE:
            case org.smartregister.chw.anc.util.Constants.EVENT_TYPE.ANC_HOME_VISIT_NOT_DONE_UNDO:
            case CoreConstants.EventType.PNC_HOME_VISIT:
            case CoreConstants.EventType.PNC_HOME_VISIT_NOT_DONE:
            case FamilyPlanningConstants.EVENT_TYPE.FP_FOLLOW_UP_VISIT:
            case FamilyPlanningConstants.EVENT_TYPE.FP_REGISTRATION:
            case org.smartregister.chw.tb.util.Constants.EventType.FOLLOW_UP_VISIT:
            case org.smartregister.chw.hiv.util.Constants.EventType.FOLLOW_UP_VISIT:
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_OUTLET_VISIT:
                if (eventClient.getEvent() == null) {
                    return;
                }
                processVisitEvent(eventClient);
                processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                break;
            case CoreConstants.EventType.REMOVE_FAMILY:
                if (eventClient.getClient() == null) {
                    return;
                }
                processRemoveFamily(eventClient.getClient().getBaseEntityId(), event.getEventDate().toDate());
                break;
            case CoreConstants.EventType.REMOVE_MEMBER:
                if (eventClient.getClient() == null) {
                    return;
                }
                processRemoveMember(eventClient.getClient().getBaseEntityId(), event);
                break;
            case Constants.EVENT_TYPE.MALARIA_FOLLOW_UP_VISIT:
                clientProcessByObs(eventClient, clientClassification, event, "fever_still", "Yes");
                if (eventClient.getClient() == null) {
                    return;
                }
                processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                break;
            case CoreConstants.EventType.STOCK_USAGE_REPORT:
                clientProcessStockEvent(event);
                break;
            case CoreConstants.EventType.REMOVE_COMMUNITY_RESPONDER:
                CommunityResponderRepository repo = CoreChwApplication.getInstance().communityResponderRepository();
                repo.purgeCommunityResponder(event.getBaseEntityId());
                break;
            case CoreConstants.EventType.COMMUNITY_RESPONDER_REGISTRATION:
                clientProcessCommunityResponderEvent(event);
                break;
            case CoreConstants.EventType.CHW_IN_APP_REPORT_EVENT:
                clientProcessInAppReportingEvent(event);
                break;
            case CoreConstants.EventType.HF_IN_APP_REPORT_EVENT:
                clientProcessHfInAppReportingEvent(event);
                break;
            case CoreConstants.EventType.REMOVE_CHILD:
                if (eventClient.getClient() == null) {
                    return;
                }
                processRemoveChild(eventClient.getClient().getBaseEntityId(), event);
                processRemoveMember(eventClient.getClient().getBaseEntityId(), event);
                break;
            case CoreConstants.EventType.CHILD_VACCINE_CARD_RECEIVED:
                if (eventClient.getClient() == null) {
                    return;
                }
                processVisitEvent(eventClient);
                processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                break;
            case CoreConstants.EventType.CHILD_REFERRAL:
            case CoreConstants.EventType.ANC_REFERRAL:
            case CoreConstants.EventType.PNC_REFERRAL:
            case CoreConstants.EventType.CLOSE_REFERRAL:
            case CoreConstants.EventType.FAMILY_PLANNING_REFERRAL:
            case CoreConstants.EventType.MALARIA_REFERRAL:
                if (eventClient.getClient() != null) {
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    org.smartregister.util.Utils.startAsyncTask(new MalariaUtil.CloseMalariaMemberFromRegister(event.getBaseEntityId()), null);
                }
                break;
            case CoreConstants.EventType.REFERRAL_DISMISSAL:
                if (eventClient.getClient() != null) {
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                    CoreReferralUtils.completeClosedReferralTasks();
                }
                break;
            case org.smartregister.chw.anc.util.Constants.EVENT_TYPE.DELETE_EVENT:
                processDeleteEvent(eventClient.getEvent());
                break;
            case CoreConstants.EventType.ANC_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.PNC_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.MALARIA_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.SICK_CHILD_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.FAMILY_PLANNING_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.HIV_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.TB_NOTIFICATION_DISMISSAL:
            case CoreConstants.EventType.PREGNANCY_CONFIRMATION_DISMISSAL:
                processNotificationDismissalEvent(eventClient.getEvent());
                break;
            case CoreConstants.EventType.MOTHER_CHAMPION_SBCC:
                processSBCCEvent(eventClient.getEvent());
                break;
            case SBC_HEALTH_EDUCATION_MOBILIZATION:
                processSBCMobilizationEvent(eventClient.getEvent());
                break;
            case CECAP_HEALTH_EDUCATION_MOBILIZATION:
                processCecapMobilizationEvent(eventClient.getEvent());
                break;
            case TB_LEPROSY_MOBILIZATION:
                processTbLeprosyMobilizationEvent(eventClient.getEvent());
                break;
            case SBC_MONTHLY_SOCIAL_MEDIA_REPORT:
                processSBCMonthlySocialMediaReportEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.hivst.util.Constants.EVENT_TYPE.HIVST_MOBILIZATION:
                processMobilizationEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_MOBILIZATION:
                processHpsMobilizationEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_ADVERTISEMENT_FEEDBACK:
                processHpsAdverstimentFeedbackEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_DEATH_REGISTRATION:
                processHpsDeathRegisterEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS:
                processHpsAnnualCensusRegisterEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.hts.util.Constants.EVENT_TYPE.SAMPLE_TESTING:
                processHtsSamplesEvent(eventClient.getEvent());
                break;
            case CoreConstants.EventType.ANC_PREGNANCY_CONFIRMATION:
            case CoreConstants.EventType.ANC_REGISTRATION:
            case CoreConstants.EventType.ANC_FOLLOWUP_CLIENT_REGISTRATION:
                processAncRegistrationEvent(eventClient, clientClassification);
                break;
            case CoreConstants.EventType.HIV_REGISTRATION:
            case CoreConstants.EventType.CBHS_REGISTRATION:
                processHivRegistrationEvent(eventClient, clientClassification);
                break;
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_CONDOM_ORDER:
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_ORDER_FROM_FACILITY:
                if (eventClient.getEvent() == null) {
                    return;
                }
                processCDPOrderEvent(eventClient.getEvent());
                break;
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_ORDER_FEEDBACK:
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_ORDER_FEEDBACK_OWN_COPY:
                if (eventClient.getEvent() == null) {
                    return;
                }
                processCDPOrderFeedback(eventClient.getEvent());
                break;
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_RECEIVE_FROM_FACILITY:
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_RESTOCK:
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_CONDOM_DISTRIBUTION_OUTSIDE:
            case org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_CONDOM_DISTRIBUTION_WITHIN:
                processCDPStockChanges(eventClient.getEvent());
                processVisitEvent(eventClient);
                break;
            case org.smartregister.chw.lab.util.Constants.EVENT_TYPE.LAB_MANIFEST_GENERATION:
                processCreateManifestEvent(eventClient.getEvent());
                processVisitEvent(eventClient);
                break;
            case org.smartregister.chw.lab.util.Constants.EVENT_TYPE.LAB_MANIFEST_DISPATCH:
                processDispatchManifestEvent(eventClient.getEvent());
                processVisitEvent(eventClient);
                break;
            case org.smartregister.chw.lab.util.Constants.EVENT_TYPE.LAB_SET_MANIFEST_SETTINGS:
                processLabSettingsEvent(eventClient.getEvent());
                processVisitEvent(eventClient);
                break;
            default:
                if (eventClient.getClient() != null) {
                    if (eventType.equals(CoreConstants.EventType.UPDATE_FAMILY_RELATIONS) && event.getEntityType().equalsIgnoreCase(CoreConstants.TABLE_NAME.FAMILY_MEMBER)) {
                        event.setEventType(CoreConstants.EventType.UPDATE_FAMILY_MEMBER_RELATIONS);
                    }
                    processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
                }
                break;
        }
    }

    private void processHivRegistrationEvent(EventClient eventClient, ClientClassification clientClassification) throws Exception {
        if (eventClient.getClient() == null) {
            return;
        }
        String baseEntityId = eventClient.getClient().getBaseEntityId();
        if (CoreHivDao.isHivMember(baseEntityId)) {
            //this deletes from ec_hiv_register, ec_cbhs_register, ec_hiv_outcome if the client was initially tested negative and now re-registering
            CoreHivDao.cleanAncDataForClient(baseEntityId);
        }
        processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
        processVisitEvent(eventClient);
    }

    private void processAncRegistrationEvent(EventClient eventClient, ClientClassification clientClassification) throws Exception {
        if (eventClient.getClient() == null) {
            return;
        }
        String baseEntityId = eventClient.getClient().getBaseEntityId();
        if (AncDao.isRestartAncCase(baseEntityId)) {
            AncDao.cleanAncDataForClient(baseEntityId);
            AncDao.incrementPregnancyNumber(baseEntityId);
        }
        processVisitEvent(eventClient);
        processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
    }

    public void processDeleteEvent(Event event) {
        try {
            // delete from vaccine table
            EventDao.deleteVaccineByFormSubmissionId(event.getFormSubmissionId());
            // delete from visit table
            EventDao.deleteVisitByFormSubmissionId(event.getFormSubmissionId());
            // delete from recurring service table
            EventDao.deleteServiceByFormSubmissionId(event.getFormSubmissionId());

            Timber.d("Ending processDeleteEvent: %s", event.getEventId());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public boolean saveReportDateSent() {
        return true;
    }

    public boolean processHfReportEvents() {
        return false;
    }

    private void clientProcessInAppReportingEvent(Event event) {
        List<Obs> reportObs = event.getObs();
        MonthlyTally report = ReportUtils.getMonthlyTallyFromObs(reportObs);
        if (report != null) {
            MonthlyTalliesRepository monthlyTalliesRepository = CoreChwApplication.getInstance().monthlyTalliesRepository();
            String formSubmissionId = event.getFormSubmissionId();
            report.setSubmissionId(formSubmissionId);
            report.setProviderId(event.getProviderId());
            if (!saveReportDateSent() && event.getEventType().equals(CoreConstants.EventType.CHW_IN_APP_REPORT_EVENT))
                report.setDateSent(null);
            monthlyTalliesRepository.save(report);
        }
    }

    private void clientProcessHfInAppReportingEvent(Event event) {
        if (processHfReportEvents())
            clientProcessInAppReportingEvent(event);
    }

    private void clientProcessStockEvent(Event event) {
        List<Obs> stockObs = event.getObs();
        StockUsage usage = ReportUtils.getStockUsageFromObs(stockObs);
        if (usage != null) {
            StockUsageReportRepository repo = CoreChwApplication.getInstance().getStockUsageRepository();
            String formSubmissionId = event.getFormSubmissionId();
            usage.setId(formSubmissionId);
            repo.addOrUpdateStockUsage(usage);
        }
    }

    private void processNotificationDismissalEvent(Event event) {
        List<Obs> notificationObs = event.getObs();
        String notificationId = null;
        String dateMarkedAsDone = null;
        if (notificationObs.size() > 0) {
            for (Obs obs : notificationObs) {
                if (CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.NOTIFICATION_ID.equals(obs.getFormSubmissionField())) {
                    notificationId = (String) obs.getValue();
                } else if (CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.DATE_NOTIFICATION_MARKED_AS_DONE.equals(obs.getFormSubmissionField())) {
                    dateMarkedAsDone = (String) obs.getValue();
                }
            }
            if (StringUtils.isBlank(dateMarkedAsDone)) {
                dateMarkedAsDone = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(event.getDateCreated().toDate());
            }
            ChwNotificationDao.markNotificationAsDone(getContext(), notificationId, event.getEntityType(), dateMarkedAsDone);
        }
    }

    private void processSBCCEvent(Event event) {
        List<Obs> sbcObs = event.getObs();
        String sbccDate = null;
        String sbccLocationType = null;
        String sbccParticipantsNumber = null;
        if (sbcObs.size() > 0) {
            for (Obs obs : sbcObs) {
                if (CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.SBCC_PARTICIPANTS_NUMBER.equals(obs.getFormSubmissionField())) {
                    sbccParticipantsNumber = (String) obs.getValue();
                } else if (CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.SBCC_LOCATION_TYPE.equals(obs.getFormSubmissionField())) {
                    sbccLocationType = (String) obs.getValue();
                } else if (CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.SBCC_DATE.equals(obs.getFormSubmissionField())) {
                    sbccDate = (String) obs.getValue();
                }
            }

            SbccDao.updateData(event.getBaseEntityId(), sbccDate, sbccLocationType, sbccParticipantsNumber);
        }
    }

    private void processSBCMobilizationEvent(Event event) {
        List<Obs> sbcObs = event.getObs();
        SbcDao.SbcMobilization sbcMobilization = new SbcDao.SbcMobilization();
        if (sbcObs.size() > 0) {
            for (Obs obs : sbcObs) {
                if ("mobilization_date".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setMobilizationDate((String) obs.getValue());
                } else if ("community_sbc_activity_provided".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setCommunitySbcActivityProvided(obs.getValues().toString());
                } else if ("other_interventions_iec_materials_distributed".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setOtherInterventionsIecMaterialsDistributed(obs.getValues().toString());
                } else if ("number_audio_visuals_distributed".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberAudioVisualsDistributed((String) obs.getValue());
                } else if ("number_audio_distributed".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberAudioDistributed((String) obs.getValue());
                } else if ("number_print_materials_distributed".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPrintMaterialsDistributed((String) obs.getValue());
                } else if ("pmtct_iec_materials_distributed".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setPmtctIecMaterialsDistributed(obs.getValues().toString());
                } else if ("number_pmtct_audio_visuals_distributed_male".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPmtctAudioVisualsDistributedMale((String) obs.getValue());
                } else if ("number_pmtct_audio_visuals_distributed_female".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPmtctAudioVisualsDistributedFemale((String) obs.getValue());
                } else if ("number_pmtct_audio_distributed_male".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPmtctAudioDistributedMale((String) obs.getValue());
                } else if ("number_pmtct_audio_distributed_female".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPmtctAudioDistributedFemale((String) obs.getValue());
                } else if ("number_pmtct_print_materials_distributed_male".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPmtctPrintMaterialsDistributedMale((String) obs.getValue());
                } else if ("number_pmtct_print_materials_distributed_female".equals(obs.getFormSubmissionField())) {
                    sbcMobilization.setNumberPmtctPrintMaterialsDistributedFemale((String) obs.getValue());
                }
            }

            SbcDao.updateSbcMobilization(sbcMobilization);
        }
    }

    private void processSBCMonthlySocialMediaReportEvent(Event event) {
        List<Obs> sbcObs = event.getObs();
        String reporting_month = null;
        String organization_name = null;
        String other_organization_name = null;
        String social_media_hiv_msg_distribution = null;
        String number_beneficiaries_reached_facebook = null;
        String number_messages_publications = null;
        String number_aired_messages_broadcasted = null;


        if (sbcObs.size() > 0) {
            for (Obs obs : sbcObs) {
                if ("reporting_month".equals(obs.getFormSubmissionField())) {
                    reporting_month = (String) obs.getValue();
                } else if ("organization_name".equals(obs.getFormSubmissionField())) {
                    organization_name = obs.getValues().toString();
                } else if ("other_organization_name".equals(obs.getFormSubmissionField())) {
                    other_organization_name = obs.getValues().toString();
                } else if ("social_media_hiv_msg_distribution".equals(obs.getFormSubmissionField())) {
                    social_media_hiv_msg_distribution = (String) obs.getValue();
                } else if ("number_beneficiaries_reached_facebook".equals(obs.getFormSubmissionField())) {
                    number_beneficiaries_reached_facebook = (String) obs.getValue();
                } else if ("number_messages_publications".equals(obs.getFormSubmissionField())) {
                    number_messages_publications = (String) obs.getValue();
                } else if ("number_aired_messages_broadcasted".equals(obs.getFormSubmissionField())) {
                    number_aired_messages_broadcasted = obs.getValues().toString();
                }
            }

            SbcDao.updateSbcSocialMediaMonthlyReport(event.getBaseEntityId(),
                    reporting_month,
                    organization_name,
                    other_organization_name,
                    social_media_hiv_msg_distribution,
                    number_beneficiaries_reached_facebook,
                    number_messages_publications,
                    number_aired_messages_broadcasted
            );
        }
    }

    private void processMobilizationEvent(Event event) {
        List<Obs> mobilizationObs = event.getObs();
        String mobilizationDate = null;
        String femaleClientsReached = null;
        String maleClientsReached = null;
        String femaleCondomsIssued = null;
        String maleCondomsIssued = null;

        if (!mobilizationObs.isEmpty()) {
            for (Obs obs : mobilizationObs) {
                if (org.smartregister.chw.hivst.util.DBConstants.KEY.MOBILIZATION_DATE.equals(obs.getFormSubmissionField())) {
                    mobilizationDate = (String) obs.getValue();
                } else if (org.smartregister.chw.hivst.util.DBConstants.KEY.FEMALE_CLIENTS_REACHED.equals(obs.getFormSubmissionField())) {
                    femaleClientsReached = (String) obs.getValue();
                } else if (org.smartregister.chw.hivst.util.DBConstants.KEY.MALE_CLIENTS_REACHED.equals(obs.getFormSubmissionField())) {
                    maleClientsReached = (String) obs.getValue();
                } else if (org.smartregister.chw.hivst.util.DBConstants.KEY.MALE_CONDOMS_ISSUED.equals(obs.getFormSubmissionField())) {
                    maleCondomsIssued = (String) obs.getValue();
                } else if (org.smartregister.chw.hivst.util.DBConstants.KEY.FEMALE_CONDOMS_ISSUED.equals(obs.getFormSubmissionField())) {
                    femaleCondomsIssued = (String) obs.getValue();
                }
            }
            HivstMobilizationDao.updateData(event.getBaseEntityId(), mobilizationDate, femaleClientsReached, maleClientsReached, maleCondomsIssued, femaleCondomsIssued);
        }
    }

    private void processHpsMobilizationEvent(Event event) {
        List<Obs> mobilizationObs = event.getObs();

        String dateOfGathering = null;
        String methodOfEducationAndAwarenessUsed = null;
        String areaWhereMobilizationTookPlace = null;
        String numberOfFemalesWhoAttended = null;
        String numberOfMalesWhoAttended = null;
        String wasEducationProvided = null;
        String educationProvided = null;
        String informationEducationAndCommunicationMaterial = null;
        String brochureMaterials = null;
        String numberOfBrochuresProvided = null;
        String posterMaterials = null;
        String numberOfPostersProvided = null;
        String leafletMaterials = null;
        String numberOfLeafletProvided = null;
        String otherIecMaterials = null;
        String numberOfOtherIecProvided = null;

        // Assuming event.getVersion() returns a long representing last_interacted_with.
        long lastInteractedWith = event.getVersion();

        if (mobilizationObs != null && !mobilizationObs.isEmpty()) {
            for (Obs obs : mobilizationObs) {
                String field = obs.getFormSubmissionField();
                if (org.smartregister.chw.hps.util.DBConstants.KEY.DATE_OF_GATHERING.equals(field)) {
                    dateOfGathering = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.METHOD_OF_EDUCATION_AND_AWARENESS_USED.equals(field)) {
                    methodOfEducationAndAwarenessUsed = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AREA_WHERE_MOBILIZATION_TAKES_PLACE.equals(field)) {
                    areaWhereMobilizationTookPlace = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALES_WHO_ATTENDED.equals(field)) {
                    numberOfFemalesWhoAttended = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALES_WHO_ATTENDED.equals(field)) {
                    numberOfMalesWhoAttended = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.WAS_EDUCATION_PROVIDED.equals(field)) {
                    wasEducationProvided = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.EDUCATION_PROVIDED.equals(field)) {
                    educationProvided = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.INFORMATION_EDUCATION_AND_COMMUNICATION_MATERIAL.equals(field)) {
                    informationEducationAndCommunicationMaterial = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.BROCHURE_MATERIALS.equals(field)) {
                    brochureMaterials = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BROCHURES_PROVIDED.equals(field)) {
                    numberOfBrochuresProvided = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.POSTER_MATERIALS.equals(field)) {
                    posterMaterials = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_POSTERS_PROVIDED.equals(field)) {
                    numberOfPostersProvided = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.LEAFLET_MATERIALS.equals(field)) {
                    leafletMaterials = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LEAFLET_PROVIDED.equals(field)) {
                    numberOfLeafletProvided = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.OTHER_IEC_MATERIALS.equals(field)) {
                    otherIecMaterials = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_OTHER_IEC_PROVIDED.equals(field)) {
                    numberOfOtherIecProvided = (String) obs.getValue();
                }
            }

            // Call the function to save the mobilization data.
            HpsDao.saveHpsMobilization(event.getBaseEntityId(),
                    dateOfGathering,
                    methodOfEducationAndAwarenessUsed,
                    areaWhereMobilizationTookPlace,
                    numberOfFemalesWhoAttended,
                    numberOfMalesWhoAttended,
                    wasEducationProvided,
                    educationProvided,
                    informationEducationAndCommunicationMaterial,
                    brochureMaterials,
                    numberOfBrochuresProvided,
                    posterMaterials,
                    numberOfPostersProvided,
                    leafletMaterials,
                    numberOfLeafletProvided,
                    otherIecMaterials,
                    numberOfOtherIecProvided,
                    lastInteractedWith);
        }
    }

    private void processHpsAdverstimentFeedbackEvent(Event event) {
        List<Obs> hpsAdverstimentFeedbackObs = event.getObs();

        String dateOfAdvertisementFeedback = null;
        String areaWhereAdvertisementFeedbackTookPlace = null;
        String numberOfFemalesWhoAttended = null;
        String numberOfMalesWhoAttended = null;
        String ownsRadioStation = null;
        String numberOfRadioOwners = null;
        String mobilePhoneRadioListeners = null;
        String numberOfWhoListensRadioViaPhone = null;
        String radioChannelListened = null;
        String selectedHealthEducationTopics = null;
        String healthEducationHeard = null;

        // Assuming event.getVersion() returns a long representing last_interacted_with.
        long lastInteractedWith = event.getVersion();

        if (hpsAdverstimentFeedbackObs != null && !hpsAdverstimentFeedbackObs.isEmpty()) {
            for (Obs obs : hpsAdverstimentFeedbackObs) {
                String field = obs.getFormSubmissionField();
                if (org.smartregister.chw.hps.util.DBConstants.KEY.DATE_OF_ADVERTISEMENT_FEEDBACK.equals(field)) {
                    dateOfAdvertisementFeedback = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AREA_WHERE_ADVERTISEMENT_FEEDBACK_TOOK_PLACE.equals(field)) {
                    areaWhereAdvertisementFeedbackTookPlace = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALES_WHO_ATTENDED.equals(field)) {
                    numberOfFemalesWhoAttended = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALES_WHO_ATTENDED.equals(field)) {
                    numberOfMalesWhoAttended = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.OWNS_RADIO_STATION.equals(field)) {
                    ownsRadioStation = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_RADIO_OWNERS.equals(field)) {
                    numberOfRadioOwners = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.MOBILE_PHONE_RADIO_LISTENERS.equals(field)) {
                    mobilePhoneRadioListeners = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_WHO_LISTENS_RADIO_VIA_PHONE.equals(field)) {
                    numberOfWhoListensRadioViaPhone = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.RADIO_CHANNEL_LISTENED.equals(field)) {
                    radioChannelListened = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.SELECTED_HEALTH_EDUCATION_TOPICS.equals(field)) {
                    selectedHealthEducationTopics = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_EDUCATION_HEARD.equals(field)) {
                    healthEducationHeard = obs.getValues().toString();
                }
            }

            // Call the function to save the mobilization data.
            HpsDao.saveHpsAdvertisementFeedback(event.getBaseEntityId(),
                    dateOfAdvertisementFeedback,
                    areaWhereAdvertisementFeedbackTookPlace,
                    numberOfFemalesWhoAttended,
                    numberOfMalesWhoAttended,
                    ownsRadioStation,
                    numberOfRadioOwners,
                    mobilePhoneRadioListeners,
                    numberOfWhoListensRadioViaPhone,
                    radioChannelListened,
                    selectedHealthEducationTopics,
                    healthEducationHeard,
                    lastInteractedWith);
        }
    }

    private void processHpsDeathRegisterEvent(Event event) {
        List<Obs> deathObs = event.getObs();

        String dod = null;
        String firstName = null;
        String middleName = null;
        String lastName = null;
        String dob = null;
        String sex = null;
        String causeOfDeath = null;
        String causeOfDeathSpecify = null;

        // Assuming event.getVersion() returns a long representing last_interacted_with.
        long lastInteractedWith = event.getVersion();

        if (deathObs != null && !deathObs.isEmpty()) {
            for (Obs obs : deathObs) {
                String field = obs.getFormSubmissionField();
                if (DBConstants.KEY.DOD.equals(field)) {
                    dod = (String) obs.getValue();
                } else if (DBConstants.KEY.FIRST_NAME.equals(field)) {
                    firstName = (String) obs.getValue();
                } else if (DBConstants.KEY.MIDDLE_NAME.equals(field)) {
                    middleName = (String) obs.getValue();
                } else if (DBConstants.KEY.LAST_NAME.equals(field)) {
                    lastName = (String) obs.getValue();
                } else if (DBConstants.KEY.DOB.equals(field)) {
                    dob = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.SEX.equals(field)) {
                    sex = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.CAUSE_OF_DEATH.equals(field)) {
                    causeOfDeath = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.CAUSE_OF_DEATH_SPECIFY.equals(field)) {
                    causeOfDeathSpecify = (String) obs.getValue();
                }
            }
            // Use formSubmissionId as the base_entity_id for the death register
            HpsDao.saveHpsDeathRegister(event.getBaseEntityId(), dod, firstName, middleName, lastName, dob, sex, causeOfDeath, causeOfDeathSpecify, lastInteractedWith);
        }
    }

    private void processHpsAnnualCensusRegisterEvent(Event event) {
        List<Obs> censusObs = event.getObs();

        // Declare variables for all fields defined in HpsAnnualCensusRegisterModel
        String year = null;
        String selectAgeGroup = null;
        String numberOfMaleByAgeGroupUnder1 = null;
        String numberOfFemaleByAgeGroupUnder1 = null;
        String numberOfMaleByAgeGroup1_4 = null;
        String numberOfFemaleByAgeGroup1_4 = null;
        String numberOfMaleByAgeGroup5_14 = null;
        String numberOfFemaleByAgeGroup5_14 = null;
        String numberOfMaleByAgeGroup15_49 = null;
        String numberOfFemaleByAgeGroup15_49 = null;
        String numberOfMaleByAgeGroup50_59 = null;
        String numberOfFemaleByAgeGroup50_59 = null;
        String numberOfMaleByAgeGroup60Plus = null;
        String numberOfFemaleByAgeGroup60Plus = null;
        String numberOfHouseHoldWithRoadAccess = null;
        String numberOfHouseHoldsWithAtLeastOneLandlineOrMobilePhone = null;
        String numberOfHouseHoldWithBasicNutritionSourceVegetable = null;
        String numberOfHouseHoldWithBasicNutritionSourceFruitTrees = null;
        String numberOfHouseHoldWithBasicNutritionSourceDomesticAnimal = null;
        String selectCentersCategory = null;
        String numberOfPreSchoolsGovernment = null;
        String numberOfPrimarySchoolsGovernment = null;
        String numberOfSecondarySchoolsGovernment = null;
        String numberOfUniversitiesGovernment = null;
        String numberOfSpecialNeedsSchoolsGovernment = null;
        String numberOfDispensaryGovernment = null;
        String numberOfHealthCentersGovernment = null;
        String numberOfHospitalGovernment = null;
        String numberOfSpecialClinicsGovernment = null;
        String numberOfLaboratoryGovernment = null;
        String numberOfPharmacyGovernment = null;
        String numberOfADDOGovernment = null;
        String numberOfMaternityHomeGovernment = null;
        String numberOfOrphanCareCentersGovernment = null;
        String numberOfCentersForChildrenWithDisabilitiesGovernment = null;
        String numberOfCbecdcRehabilitationCentreGovernment = null;
        String numberOfDayCareCentersGovernment = null;
        String numberOfElderlyCareCentersGovernment = null;
        String numberOfPreSchoolsFaithBasedOrganisation = null;
        String numberOfPrimarySchoolsFaithBasedOrganisation = null;
        String numberOfSecondarySchoolsFaithBasedOrganisation = null;
        String numberOfUniversitiesFaithBasedOrganisation = null;
        String numberOfSpecialNeedsSchoolsFaithBasedOrganisation = null;
        String numberOfDispensaryFaithBasedOrganisation = null;
        String numberOfHealthCentersFaithBasedOrganisation = null;
        String numberOfHospitalFaithBasedOrganisation = null;
        String numberOfSpecialClinicsFaithBasedOrganisation = null;
        String numberOfLaboratoryFaithBasedOrganisation = null;
        String numberOfPharmacyFaithBasedOrganisation = null;
        String numberOfADDOFaithBasedOrganisation = null;
        String numberOfMaternityHomeFaithBasedOrganisation = null;
        String numberOfOrphanCareCentersFaithBasedOrganisation = null;
        String numberOfCentersForChildrenWithDisabilitiesFaithBasedOrganisation = null;
        String numberOfCbecdcRehabilitationCentreFaithBasedOrganisation = null;
        String numberOfDayCareCentersFaithBasedOrganisation = null;
        String numberOfElderlyCareCentersFaithBasedOrganisation = null;
        String numberOfPreSchoolsPublic = null;
        String numberOfPrimarySchoolsPublic = null;
        String numberOfSecondarySchoolsPublic = null;
        String numberOfUniversitiesPublic = null;
        String numberOfSpecialNeedsSchoolsPublic = null;
        String numberOfDispensaryPublic = null;
        String numberOfHealthCentersPublic = null;
        String numberOfHospitalPublic = null;
        String numberOfSpecialClinicsPublic = null;
        String numberOfLaboratoryPublic = null;
        String numberOfPharmacyPublic = null;
        String numberOfADDOPublic = null;
        String numberOfMaternityHomePublic = null;
        String numberOfOrphanCareCentersPublic = null;
        String numberOfCentersForChildrenWithDisabilitiesPublic = null;
        String numberOfCbecdcRehabilitationCentrePublic = null;
        String numberOfDayCareCentersPublic = null;
        String numberOfElderlyCareCentersPublic = null;
        String numberOfPreSchoolsPrivate = null;
        String numberOfPrimarySchoolsPrivate = null;
        String numberOfSecondarySchoolsPrivate = null;
        String numberOfUniversitiesPrivate = null;
        String numberOfSpecialNeedsSchoolsPrivate = null;
        String numberOfDispensaryPrivate = null;
        String numberOfHealthCentersPrivate = null;
        String numberOfHospitalPrivate = null;
        String numberOfSpecialClinicsPrivate = null;
        String numberOfLaboratoryPrivate = null;
        String numberOfPharmacyPrivate = null;
        String numberOfADDOPrivate = null;
        String numberOfMaternityHomePrivate = null;
        String numberOfOrphanCareCentersPrivate = null;
        String numberOfCentersForChildrenWithDisabilitiesPrivate = null;
        String numberOfCbecdcRehabilitationCentrePrivate = null;
        String numberOfDayCareCentersPrivate = null;
        String numberOfElderlyCareCentersPrivate = null;
        String numberOfFoodShopVisited = null;
        String numberOfRestaurantsVisited = null;
        String numberOfButcheriesVisited = null;
        String numberOfBarAndClubsVisited = null;
        String numberOfGuestHouseVisited = null;
        String numberOfLocaFoodVendorsVisited = null;
        String numberOfMarketsVisited = null;
        String numberOfPublicToiletsVisited = null;
        String numberOfBusStationsVisited = null;
        String numberOfPrimarySchoolsVisited = null;
        String numberOfSecondarySchoolsVisited = null;
        String numberOfHospitalVisited = null;
        String numberOfHealthCentersVisited = null;
        String numberOfDispensariesVisited = null;
        String numberOfOfficesVisited = null;
        String numberOfUniversitiesCollegeVisited = null;
        String numberOfFoodShopThatMetTheStandards = null;
        String numberOfRestaurantsThatMetTheStandards = null;
        String numberOfButcheriesThatMetTheStandards = null;
        String numberOfBarAndClubsThatMetTheStandards = null;
        String numberOfGuestHouseThatMetTheStandards = null;
        String numberOfLocaFoodVendorsThatMetTheStandards = null;
        String numberOfMarketsThatMetTheStandards = null;
        String numberOfPublicToiletsThatMetTheStandards = null;
        String numberOfBusStationsThatMetTheStandards = null;
        String numberOfPrimarySchoolsThatMetTheStandards = null;
        String numberOfSecondarySchoolsThatMetTheStandards = null;
        String numberOfHospitalThatMetTheStandards = null;
        String numberOfHealthCentersThatMetTheStandards = null;
        String numberOfDispensariesThatMetTheStandards = null;
        String numberOfOfficesThatMetTheStandards = null;
        String numberOfUniversitiesCollegeThatMetTheStandards = null;
        String numberOfInspectedAgricultureAreas = null;
        String numberOfInspectedLivestockKeepingAreas = null;
        String numberOfInspectedFishingAreas = null;
        String numberOfInspectedIndustriesAreas = null;
        String numberOfInspectedOfficesAreas = null;
        String numberOfInspectedTransportationAreas = null;
        String numberOfOtherInspectedAreas = null;
        String numberOfAgricultureAreasInspectedWithRiskIndicators = null;
        String numberOfLivestockKeepingAreasInspectedWithRiskIndicators = null;
        String numberOfFishingAreasInspectedWithRiskIndicators = null;
        String numberOfIndustriesAreasInspectedWithRiskIndicators = null;
        String numberOfOfficesAreasInspectedWithRiskIndicators = null;
        String numberOfTransportationAreasInspectedWithRiskIndicators = null;
        String numberOfOtherAreasInspectedWithRiskIndicators = null;
        String numberOfInspectedGrains = null;
        String numberOfInspectedLegumes = null;
        String numberOfInspectedMeat = null;
        String numberOfInspectedFishing = null;
        String numberOfInspectedAlcoholicBeverages = null;
        String numberOfInspectedNonAlcoholicBeverages = null;
        String numberOfGrainsDiscarded = null;
        String numberOfLegumesDiscarded = null;
        String numberOfMeatDiscarded = null;
        String numberOfFishingDiscarded = null;
        String numberOfAlcoholicBeverageDiscarded = null;
        String numberOfNonAlcoholicBeverageDiscarded = null;
        String healthReportsAffectingPeopleInWorkplacesRespiratoryDiseases = null;
        String healthReportsAffectingPeopleInWorkplacesToxicChemicals = null;
        String healthReportsAffectingPeopleInWorkplacesBurns = null;
        String healthReportsAffectingPeopleInWorkplacesHearingLoss = null;
        String healthReportsAffectingPeopleInWorkplacesEyeProblems = null;
        String healthReportsAffectingPeopleInWorkplacesOtherEffects = null;
        String amountOfSolidWasteGeneratedAnnuallyTons = null;
        String amountOfSolidWasteDisposedAtADesignatedSiteAnnuallyTons = null;
        String numberOfWasteCollectionEquipmentVehicles = null;
        String numberOfWasteCollectionEquipmentTractors = null;
        String numberOfWasteCollectionEquipmentCarts = null;
        String numberOfWasteCollectionEquipmentWheelbarrows = null;
        String numberOfWasteCollectionEquipmentOthers = null;
        String numberOfAreasSprayedWithPesticidesPonds = null;
        String numberOfAreasSprayedWithPesticidesCans = null;
        String numberOfAreasSprayedWithPesticidesDrums = null;
        String numberOfAreasSprayedWithPesticidesBarrels = null;
        String numberOfAreasSprayedWithPesticidesCoconutShells = null;
        String numberOfTimesSprayingWasDonePonds = null;
        String numberOfTimesSprayingWasDoneCans = null;
        String numberOfTimesSprayingWasDoneDrums = null;
        String numberOfTimesSprayingWasDoneBarrels = null;
        String numberOfTimesSprayingWasDoneCoconutShells = null;
        String typesOfPesticidesUsedPonds = null;
        String typesOfPesticidesUsedCans = null;
        String typesOfPesticidesUsedDrums = null;
        String typesOfPesticidesUsedBarrels = null;
        String typesOfPesticidesUsedCoconutShells = null;
        String amountOfPesticideUsedPonds = null;
        String amountOfPesticideUsedCans = null;
        String amountOfPesticideUsedDrums = null;
        String amountOfPesticideUsedBarrels = null;
        String amountOfPesticideUsedCoconutShells = null;
        String numberOfHouseHolds = null;
        String numberOfMaleCapableOfEngagingInEconomicActivities = null;
        String numberOfFemaleCapableOfEngagingInEconomicActivities = null;
        String numberOfMaleEngagedInEconomicActivities = null;
        String numberOfFemaleEngagedInEconomicActivities = null;
        String numberOfHouseholdsMostCommonlyUseTapAsSourcesOfWater = null;
        String numberOfHouseholdsMostCommonlyUseRiverAsSourcesOfWater = null;
        String numberOfHouseholdsMostCommonlyUseShallowWellAsSourcesOfWater = null;
        String numberOfHouseholdsMostCommonlyUseWaterPondAsSourcesOfWater = null;
        String numberOfHouseholdsMostCommonlyUseSmallDamAsSourcesOfWater = null;
        String numberOfHouseholdsMostCommonlyUseLakeAsSourcesOfWater = null;
        String numberOfHouseholdsMostCommonlyUseSpringAsSourcesOfWater = null;
        String numberOfHouseholdsUsingElectricityAsSourceOfEnergyForLighting = null;
        String numberOfHouseholdsUsingSolarAsSourceOfEnergyForLighting = null;
        String numberOfHouseholdsUsingKerosineAsSourceOfEnergyForLighting = null;
        String numberOfHouseholdsUsingKoroboiAsSourceOfEnergyForLighting = null;
        String numberOfHouseholdsUsingOtherSourceOfEnergyForLighting = null;
        String numberOfHouseholdsUsingElectricityAsSourceOfCookingEnergy = null;
        String numberOfHouseholdsUsingSolarAsSourceOfCookingEnergy = null;
        String numberOfHouseholdsUsingKerosineAsSourceOfCookingEnergy = null;
        String numberOfHouseholdsUsingGasAsSourceOfCookingEnergy = null;
        String numberOfHouseholdsUsingCharcoalAsSourceOfCookingEnergy = null;
        String numberOfHouseholdsUsingFirewoodAsSourceOfCookingEnergy = null;
        String numberOfHealthCommitteeMembersForEffectiveCommitteeMeetings = null;
        String numberOfCommitteeMembersAttendedFisrtQuarter = null;
        String numberOfRegisteredAlternativeMedicineServiceProviders = null;
        String numberOfRegisteredTraditionalMedicineServiceProviders = null;
        String numberOfUnregisteredAlternativeMedicineServiceProviders = null;
        String numberOfUnregisteredTraditionalMedicineServiceProviders = null;


        // Retrieve the last_interacted_with value from the event version
        long lastInteractedWith = event.getVersion();

        // Process each observation in the event
        if (censusObs != null && !censusObs.isEmpty()) {
            for (Obs obs : censusObs) {
                String field = obs.getFormSubmissionField();
                if (org.smartregister.chw.hps.util.DBConstants.KEY.YEAR.equals(field)) {
                    year = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.SELECT_AGE_GROUP.equals(field)) {
                    selectAgeGroup = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_BY_AGE_GROUP_UNDER1.equals(field)) {
                    numberOfMaleByAgeGroupUnder1 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_BY_AGE_GROUP_UNDER1.equals(field)) {
                    numberOfFemaleByAgeGroupUnder1 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_BY_AGE_GROUP_1_4.equals(field)) {
                    numberOfMaleByAgeGroup1_4 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_BY_AGE_GROUP_1_4.equals(field)) {
                    numberOfFemaleByAgeGroup1_4 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_BY_AGE_GROUP_5_14.equals(field)) {
                    numberOfMaleByAgeGroup5_14 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_BY_AGE_GROUP_5_14.equals(field)) {
                    numberOfFemaleByAgeGroup5_14 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_BY_AGE_GROUP_15_49.equals(field)) {
                    numberOfMaleByAgeGroup15_49 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_BY_AGE_GROUP_15_49.equals(field)) {
                    numberOfFemaleByAgeGroup15_49 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_BY_AGE_GROUP_50_59.equals(field)) {
                    numberOfMaleByAgeGroup50_59 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_BY_AGE_GROUP_50_59.equals(field)) {
                    numberOfFemaleByAgeGroup50_59 = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_BY_AGE_GROUP_60_PLUS.equals(field)) {
                    numberOfMaleByAgeGroup60Plus = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_BY_AGE_GROUP_60_PLUS.equals(field)) {
                    numberOfFemaleByAgeGroup60Plus = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSE_HOLD_WITH_ROAD_ACCESS.equals(field)) {
                    numberOfHouseHoldWithRoadAccess = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSE_HOLDS_WITH_AT_LEAST_ONE_LANDLINE_OR_MOBILE_PHONE.equals(field)) {
                    numberOfHouseHoldsWithAtLeastOneLandlineOrMobilePhone = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSE_HOLD_WITH_BASIC_NUTRITION_SOURCE_VEGETABLE.equals(field)) {
                    numberOfHouseHoldWithBasicNutritionSourceVegetable = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSE_HOLD_WITH_BASIC_NUTRITION_SOURCE_FRUIT_TREES.equals(field)) {
                    numberOfHouseHoldWithBasicNutritionSourceFruitTrees = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSE_HOLD_WITH_BASIC_NUTRITION_SOURCE_DOMESTIC_ANIMAL.equals(field)) {
                    numberOfHouseHoldWithBasicNutritionSourceDomesticAnimal = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.SELECT_CENTERS_CATEGORY.equals(field)) {
                    selectCentersCategory = obs.getValues().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRE_SCHOOLS_GOVERNMENT.equals(field)) {
                    numberOfPreSchoolsGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRIMARY_SCHOOLS_GOVERNMENT.equals(field)) {
                    numberOfPrimarySchoolsGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SECONDARY_SCHOOLS_GOVERNMENT.equals(field)) {
                    numberOfSecondarySchoolsGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNIVERSITIES_GOVERNMENT.equals(field)) {
                    numberOfUniversitiesGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_NEEDS_SCHOOLS_GOVERNMENT.equals(field)) {
                    numberOfSpecialNeedsSchoolsGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DISPENSARY_GOVERNMENT.equals(field)) {
                    numberOfDispensaryGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_CENTERS_GOVERNMENT.equals(field)) {
                    numberOfHealthCentersGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOSPITAL_GOVERNMENT.equals(field)) {
                    numberOfHospitalGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_CLINICS_GOVERNMENT.equals(field)) {
                    numberOfSpecialClinicsGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LABORATORY_GOVERNMENT.equals(field)) {
                    numberOfLaboratoryGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PHARMACY_GOVERNMENT.equals(field)) {
                    numberOfPharmacyGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ADDO_GOVERNMENT.equals(field)) {
                    numberOfADDOGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MATERNITY_HOME_GOVERNMENT.equals(field)) {
                    numberOfMaternityHomeGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ORPHAN_CARE_CENTERS_GOVERNMENT.equals(field)) {
                    numberOfOrphanCareCentersGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CENTERS_FOR_CHILDREN_WITH_DISABILITIES_GOVERNMENT.equals(field)) {
                    numberOfCentersForChildrenWithDisabilitiesGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CBECDC_REHABILITATION_CENTRE_GOVERNMENT.equals(field)) {
                    numberOfCbecdcRehabilitationCentreGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DAY_CARE_CENTERS_GOVERNMENT.equals(field)) {
                    numberOfDayCareCentersGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ELDERLY_CARE_CENTERS_GOVERNMENT.equals(field)) {
                    numberOfElderlyCareCentersGovernment = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRE_SCHOOLS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfPreSchoolsFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRIMARY_SCHOOLS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfPrimarySchoolsFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SECONDARY_SCHOOLS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfSecondarySchoolsFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNIVERSITIES_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfUniversitiesFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_NEEDS_SCHOOLS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfSpecialNeedsSchoolsFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DISPENSARY_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfDispensaryFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_CENTERS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfHealthCentersFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOSPITAL_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfHospitalFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_CLINICS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfSpecialClinicsFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LABORATORY_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfLaboratoryFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PHARMACY_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfPharmacyFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ADDO_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfADDOFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MATERNITY_HOME_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfMaternityHomeFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ORPHAN_CARE_CENTERS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfOrphanCareCentersFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CENTERS_FOR_CHILDREN_WITH_DISABILITIES_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfCentersForChildrenWithDisabilitiesFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CBECDC_REHABILITATION_CENTRE_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfCbecdcRehabilitationCentreFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DAY_CARE_CENTERS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfDayCareCentersFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ELDERLY_CARE_CENTERS_FAITH_BASED_ORGANISATION.equals(field)) {
                    numberOfElderlyCareCentersFaithBasedOrganisation = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRE_SCHOOLS_PUBLIC.equals(field)) {
                    numberOfPreSchoolsPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRIMARY_SCHOOLS_PUBLIC.equals(field)) {
                    numberOfPrimarySchoolsPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SECONDARY_SCHOOLS_PUBLIC.equals(field)) {
                    numberOfSecondarySchoolsPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNIVERSITIES_PUBLIC.equals(field)) {
                    numberOfUniversitiesPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_NEEDS_SCHOOLS_PUBLIC.equals(field)) {
                    numberOfSpecialNeedsSchoolsPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DISPENSARY_PUBLIC.equals(field)) {
                    numberOfDispensaryPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_CENTERS_PUBLIC.equals(field)) {
                    numberOfHealthCentersPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOSPITAL_PUBLIC.equals(field)) {
                    numberOfHospitalPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_CLINICS_PUBLIC.equals(field)) {
                    numberOfSpecialClinicsPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LABORATORY_PUBLIC.equals(field)) {
                    numberOfLaboratoryPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PHARMACY_PUBLIC.equals(field)) {
                    numberOfPharmacyPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ADDO_PUBLIC.equals(field)) {
                    numberOfADDOPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MATERNITY_HOME_PUBLIC.equals(field)) {
                    numberOfMaternityHomePublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ORPHAN_CARE_CENTERS_PUBLIC.equals(field)) {
                    numberOfOrphanCareCentersPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CENTERS_FOR_CHILDREN_WITH_DISABILITIES_PUBLIC.equals(field)) {
                    numberOfCentersForChildrenWithDisabilitiesPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CBECDC_REHABILITATION_CENTRE_PUBLIC.equals(field)) {
                    numberOfCbecdcRehabilitationCentrePublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DAY_CARE_CENTERS_PUBLIC.equals(field)) {
                    numberOfDayCareCentersPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ELDERLY_CARE_CENTERS_PUBLIC.equals(field)) {
                    numberOfElderlyCareCentersPublic = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRE_SCHOOLS_PRIVATE.equals(field)) {
                    numberOfPreSchoolsPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRIMARY_SCHOOLS_PRIVATE.equals(field)) {
                    numberOfPrimarySchoolsPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SECONDARY_SCHOOLS_PRIVATE.equals(field)) {
                    numberOfSecondarySchoolsPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNIVERSITIES_PRIVATE.equals(field)) {
                    numberOfUniversitiesPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_NEEDS_SCHOOLS_PRIVATE.equals(field)) {
                    numberOfSpecialNeedsSchoolsPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DISPENSARY_PRIVATE.equals(field)) {
                    numberOfDispensaryPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_CENTERS_PRIVATE.equals(field)) {
                    numberOfHealthCentersPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOSPITAL_PRIVATE.equals(field)) {
                    numberOfHospitalPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SPECIAL_CLINICS_PRIVATE.equals(field)) {
                    numberOfSpecialClinicsPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LABORATORY_PRIVATE.equals(field)) {
                    numberOfLaboratoryPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PHARMACY_PRIVATE.equals(field)) {
                    numberOfPharmacyPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ADDO_PRIVATE.equals(field)) {
                    numberOfADDOPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MATERNITY_HOME_PRIVATE.equals(field)) {
                    numberOfMaternityHomePrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ORPHAN_CARE_CENTERS_PRIVATE.equals(field)) {
                    numberOfOrphanCareCentersPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CENTERS_FOR_CHILDREN_WITH_DISABILITIES_PRIVATE.equals(field)) {
                    numberOfCentersForChildrenWithDisabilitiesPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_CBECDC_REHABILITATION_CENTRE_PRIVATE.equals(field)) {
                    numberOfCbecdcRehabilitationCentrePrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DAY_CARE_CENTERS_PRIVATE.equals(field)) {
                    numberOfDayCareCentersPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ELDERLY_CARE_CENTERS_PRIVATE.equals(field)) {
                    numberOfElderlyCareCentersPrivate = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FOOD_SHOP_VISITED.equals(field)) {
                    numberOfFoodShopVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_RESTAURANTS_VISITED.equals(field)) {
                    numberOfRestaurantsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BUTCHERIES_VISITED.equals(field)) {
                    numberOfButcheriesVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BAR_AND_CLUBS_VISITED.equals(field)) {
                    numberOfBarAndClubsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_GUEST_HOUSE_VISITED.equals(field)) {
                    numberOfGuestHouseVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LOCA_FOOD_VENDORS_VISITED.equals(field)) {
                    numberOfLocaFoodVendorsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MARKETS_VISITED.equals(field)) {
                    numberOfMarketsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PUBLIC_TOILETS_VISITED.equals(field)) {
                    numberOfPublicToiletsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BUS_STATIONS_VISITED.equals(field)) {
                    numberOfBusStationsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRIMARY_SCHOOLS_VISITED.equals(field)) {
                    numberOfPrimarySchoolsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SECONDARY_SCHOOLS_VISITED.equals(field)) {
                    numberOfSecondarySchoolsVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOSPITAL_VISITED.equals(field)) {
                    numberOfHospitalVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_CENTERS_VISITED.equals(field)) {
                    numberOfHealthCentersVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DISPENSARIES_VISITED.equals(field)) {
                    numberOfDispensariesVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_OFFICES_VISITED.equals(field)) {
                    numberOfOfficesVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNIVERSITIES_COLLEGE_VISITED.equals(field)) {
                    numberOfUniversitiesCollegeVisited = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FOOD_SHOP_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfFoodShopThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_RESTAURANTS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfRestaurantsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BUTCHERIES_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfButcheriesThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BAR_AND_CLUBS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfBarAndClubsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_GUEST_HOUSE_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfGuestHouseThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LOCA_FOOD_VENDORS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfLocaFoodVendorsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MARKETS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfMarketsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PUBLIC_TOILETS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfPublicToiletsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_BUS_STATIONS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfBusStationsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_PRIMARY_SCHOOLS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfPrimarySchoolsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_SECONDARY_SCHOOLS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfSecondarySchoolsThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOSPITAL_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfHospitalThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_CENTERS_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfHealthCentersThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_DISPENSARIES_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfDispensariesThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_OFFICES_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfOfficesThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNIVERSITIES_COLLEGE_THAT_MET_THE_STANDARDS.equals(field)) {
                    numberOfUniversitiesCollegeThatMetTheStandards = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_AGRICULTURE_AREAS.equals(field)) {
                    numberOfInspectedAgricultureAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_LIVESTOCK_KEEPING_AREAS.equals(field)) {
                    numberOfInspectedLivestockKeepingAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_FISHING_AREAS.equals(field)) {
                    numberOfInspectedFishingAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_INDUSTRIES_AREAS.equals(field)) {
                    numberOfInspectedIndustriesAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_OFFICES_AREAS.equals(field)) {
                    numberOfInspectedOfficesAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_TRANSPORTATION_AREAS.equals(field)) {
                    numberOfInspectedTransportationAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_OTHER_INSPECTED_AREAS.equals(field)) {
                    numberOfOtherInspectedAreas = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_AGRICULTURE_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfAgricultureAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LIVESTOCK_KEEPING_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfLivestockKeepingAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FISHING_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfFishingAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INDUSTRIES_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfIndustriesAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_OFFICES_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfOfficesAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_TRANSPORTATION_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfTransportationAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_OTHER_AREAS_INSPECTED_WITH_RISK_INDICATORS.equals(field)) {
                    numberOfOtherAreasInspectedWithRiskIndicators = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_GRAINS.equals(field)) {
                    numberOfInspectedGrains = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_LEGUMES.equals(field)) {
                    numberOfInspectedLegumes = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_MEAT.equals(field)) {
                    numberOfInspectedMeat = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_FISHING.equals(field)) {
                    numberOfInspectedFishing = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_ALCOHOLIC_BEVERAGES.equals(field)) {
                    numberOfInspectedAlcoholicBeverages = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_INSPECTED_NON_ALCOHOLIC_BEVERAGES.equals(field)) {
                    numberOfInspectedNonAlcoholicBeverages = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_GRAINS_DISCARDED.equals(field)) {
                    numberOfGrainsDiscarded = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_LEGUMES_DISCARDED.equals(field)) {
                    numberOfLegumesDiscarded = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MEAT_DISCARDED.equals(field)) {
                    numberOfMeatDiscarded = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FISHING_DISCARDED.equals(field)) {
                    numberOfFishingDiscarded = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_ALCOHOLIC_BEVERAGE_DISCARDED.equals(field)) {
                    numberOfAlcoholicBeverageDiscarded = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_NON_ALCOHOLIC_BEVERAGE_DISCARDED.equals(field)) {
                    numberOfNonAlcoholicBeverageDiscarded = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_REPORTS_AFFECTING_PEOPLE_IN_WORKPLACES_RESPIRATORY_DISEASES.equals(field)) {
                    healthReportsAffectingPeopleInWorkplacesRespiratoryDiseases = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_REPORTS_AFFECTING_PEOPLE_IN_WORKPLACES_TOXIC_CHEMICALS.equals(field)) {
                    healthReportsAffectingPeopleInWorkplacesToxicChemicals = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_REPORTS_AFFECTING_PEOPLE_IN_WORKPLACES_BURNS.equals(field)) {
                    healthReportsAffectingPeopleInWorkplacesBurns = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_REPORTS_AFFECTING_PEOPLE_IN_WORKPLACES_HEARING_LOSS.equals(field)) {
                    healthReportsAffectingPeopleInWorkplacesHearingLoss = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_REPORTS_AFFECTING_PEOPLE_IN_WORKPLACES_EYE_PROBLEMS.equals(field)) {
                    healthReportsAffectingPeopleInWorkplacesEyeProblems = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.HEALTH_REPORTS_AFFECTING_PEOPLE_IN_WORKPLACES_OTHER_EFFECTS.equals(field)) {
                    healthReportsAffectingPeopleInWorkplacesOtherEffects = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_SOLID_WASTE_GENERATED_ANNUALLY_TONS.equals(field)) {
                    amountOfSolidWasteGeneratedAnnuallyTons = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_SOLID_WASTE_DISPOSED_AT_A_DESIGNATED_SITE_ANNUALLY_TONS.equals(field)) {
                    amountOfSolidWasteDisposedAtADesignatedSiteAnnuallyTons = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_WASTE_COLLECTION_EQUIPMENT_VEHICLES.equals(field)) {
                    numberOfWasteCollectionEquipmentVehicles = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_WASTE_COLLECTION_EQUIPMENT_TRACTORS.equals(field)) {
                    numberOfWasteCollectionEquipmentTractors = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_WASTE_COLLECTION_EQUIPMENT_CARTS.equals(field)) {
                    numberOfWasteCollectionEquipmentCarts = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_WASTE_COLLECTION_EQUIPMENT_WHEELBARROWS.equals(field)) {
                    numberOfWasteCollectionEquipmentWheelbarrows = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_WASTE_COLLECTION_EQUIPMENT_OTHERS.equals(field)) {
                    numberOfWasteCollectionEquipmentOthers = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_AREAS_SPRAYED_WITH_PESTICIDES_PONDS.equals(field)) {
                    numberOfAreasSprayedWithPesticidesPonds = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_AREAS_SPRAYED_WITH_PESTICIDES_CANS.equals(field)) {
                    numberOfAreasSprayedWithPesticidesCans = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_AREAS_SPRAYED_WITH_PESTICIDES_DRUMS.equals(field)) {
                    numberOfAreasSprayedWithPesticidesDrums = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_AREAS_SPRAYED_WITH_PESTICIDES_BARRELS.equals(field)) {
                    numberOfAreasSprayedWithPesticidesBarrels = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_AREAS_SPRAYED_WITH_PESTICIDES_COCONUT_SHELLS.equals(field)) {
                    numberOfAreasSprayedWithPesticidesCoconutShells = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_TIMES_SPRAYING_WAS_DONE_PONDS.equals(field)) {
                    numberOfTimesSprayingWasDonePonds = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_TIMES_SPRAYING_WAS_DONE_CANS.equals(field)) {
                    numberOfTimesSprayingWasDoneCans = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_TIMES_SPRAYING_WAS_DONE_DRUMS.equals(field)) {
                    numberOfTimesSprayingWasDoneDrums = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_TIMES_SPRAYING_WAS_DONE_BARRELS.equals(field)) {
                    numberOfTimesSprayingWasDoneBarrels = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_TIMES_SPRAYING_WAS_DONE_COCONUT_SHELLS.equals(field)) {
                    numberOfTimesSprayingWasDoneCoconutShells = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.TYPES_OF_PESTICIDES_USED_PONDS.equals(field)) {
                    typesOfPesticidesUsedPonds = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.TYPES_OF_PESTICIDES_USED_CANS.equals(field)) {
                    typesOfPesticidesUsedCans = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.TYPES_OF_PESTICIDES_USED_DRUMS.equals(field)) {
                    typesOfPesticidesUsedDrums = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.TYPES_OF_PESTICIDES_USED_BARRELS.equals(field)) {
                    typesOfPesticidesUsedBarrels = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.TYPES_OF_PESTICIDES_USED_COCONUT_SHELLS.equals(field)) {
                    typesOfPesticidesUsedCoconutShells = (String) obs.getValue();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_PESTICIDE_USED_PONDS.equals(field)) {
                    amountOfPesticideUsedPonds = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_PESTICIDE_USED_CANS.equals(field)) {
                    amountOfPesticideUsedCans = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_PESTICIDE_USED_DRUMS.equals(field)) {
                    amountOfPesticideUsedDrums = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_PESTICIDE_USED_BARRELS.equals(field)) {
                    amountOfPesticideUsedBarrels = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.AMOUNT_OF_PESTICIDE_USED_COCONUT_SHELLS.equals(field)) {
                    amountOfPesticideUsedCoconutShells = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSE_HOLD.equals(field)) {
                    numberOfHouseHolds = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_CAPABLE_OF_ENGAGING_IN_ECONOMIC_ACTIVITIES.equals(field)) {
                    numberOfMaleCapableOfEngagingInEconomicActivities = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_CAPABLE_OF_ENGAGING_IN_ECONOMIC_ACTIVITIES.equals(field)) {
                    numberOfFemaleCapableOfEngagingInEconomicActivities = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_MALE_ENGAGED_IN_ECONOMIC_ACTIVITIES.equals(field)) {
                    numberOfMaleEngagedInEconomicActivities = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_FEMALE_ENGAGED_IN_ECONOMIC_ACTIVITIES.equals(field)) {
                    numberOfFemaleEngagedInEconomicActivities = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_TAP_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseTapAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_RIVER_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseRiverAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_SHALLOW_WELL_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseShallowWellAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_WATER_POND_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseWaterPondAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_SMALL_DAM_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseSmallDamAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_LAKE_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseLakeAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_MOST_COMMONLY_USE_SPRING_AS_SOURCES_OF_WATER.equals(field)) {
                    numberOfHouseholdsMostCommonlyUseSpringAsSourcesOfWater = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_ELECTRICITY_AS_SOURCE_OF_ENERGY_FOR_LIGHTING.equals(field)) {
                    numberOfHouseholdsUsingElectricityAsSourceOfEnergyForLighting = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_SOLAR_AS_SOURCE_OF_ENERGY_FOR_LIGHTING.equals(field)) {
                    numberOfHouseholdsUsingSolarAsSourceOfEnergyForLighting = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_KEROSINE_AS_SOURCE_OF_ENERGY_FOR_LIGHTING.equals(field)) {
                    numberOfHouseholdsUsingKerosineAsSourceOfEnergyForLighting = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_KOROBOI_AS_SOURCE_OF_ENERGY_FOR_LIGHTING.equals(field)) {
                    numberOfHouseholdsUsingKoroboiAsSourceOfEnergyForLighting = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_OTHER_SOURCE_OF_ENERGY_FOR_LIGHTING.equals(field)) {
                    numberOfHouseholdsUsingOtherSourceOfEnergyForLighting = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_ELECTRICITY_AS_SOURCE_OF_COOKING_ENERGY.equals(field)) {
                    numberOfHouseholdsUsingElectricityAsSourceOfCookingEnergy = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_SOLAR_AS_SOURCE_OF_COOKING_ENERGY.equals(field)) {
                    numberOfHouseholdsUsingSolarAsSourceOfCookingEnergy = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_KEROSINE_AS_SOURCE_OF_COOKING_ENERGY.equals(field)) {
                    numberOfHouseholdsUsingKerosineAsSourceOfCookingEnergy = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_GAS_AS_SOURCE_OF_COOKING_ENERGY.equals(field)) {
                    numberOfHouseholdsUsingGasAsSourceOfCookingEnergy = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_CHARCOAL_AS_SOURCE_OF_COOKING_ENERGY.equals(field)) {
                    numberOfHouseholdsUsingCharcoalAsSourceOfCookingEnergy = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HOUSEHOLDS_USING_FIREWOOD_AS_SOURCE_OF_COOKING_ENERGY.equals(field)) {
                    numberOfHouseholdsUsingFirewoodAsSourceOfCookingEnergy = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_HEALTH_COMMITTEE_MEMBERS_FOR_EFFECTIVE_COMMITTEE_MEETINGS.equals(field)) {
                    numberOfHealthCommitteeMembersForEffectiveCommitteeMeetings = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_COMMITTEE_MEMBERS_ATTENDED_FISRT_QUARTER.equals(field)) {
                    numberOfCommitteeMembersAttendedFisrtQuarter = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_REGISTERED_ALTERNATIVE_MEDICINE_SERVICE_PROVIDERS.equals(field)) {
                    numberOfRegisteredAlternativeMedicineServiceProviders = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_REGISTERED_TRADITIONAL_MEDICINE_SERVICE_PROVIDERS.equals(field)) {
                    numberOfRegisteredTraditionalMedicineServiceProviders = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNREGISTERED_ALTERNATIVE_MEDICINE_SERVICE_PROVIDERS.equals(field)) {
                    numberOfUnregisteredAlternativeMedicineServiceProviders = obs.getValue().toString();
                } else if (org.smartregister.chw.hps.util.DBConstants.KEY.NUMBER_OF_UNREGISTERED_TRADITIONAL_MEDICINE_SERVICE_PROVIDERS.equals(field)) {
                    numberOfUnregisteredTraditionalMedicineServiceProviders = obs.getValue().toString();
                }
            }
            // Save the annual census register record using the extracted values
            HpsDao.saveHpsAnnualCensusRegisterModel(
                    event.getBaseEntityId(),
                    year,
                    selectAgeGroup,
                    numberOfMaleByAgeGroupUnder1,
                    numberOfFemaleByAgeGroupUnder1,
                    numberOfMaleByAgeGroup1_4,
                    numberOfFemaleByAgeGroup1_4,
                    numberOfMaleByAgeGroup5_14,
                    numberOfFemaleByAgeGroup5_14,
                    numberOfMaleByAgeGroup15_49,
                    numberOfFemaleByAgeGroup15_49,
                    numberOfMaleByAgeGroup50_59,
                    numberOfFemaleByAgeGroup50_59,
                    numberOfMaleByAgeGroup60Plus,
                    numberOfFemaleByAgeGroup60Plus,
                    numberOfHouseHoldWithRoadAccess,
                    numberOfHouseHoldsWithAtLeastOneLandlineOrMobilePhone,
                    numberOfHouseHoldWithBasicNutritionSourceVegetable,
                    numberOfHouseHoldWithBasicNutritionSourceFruitTrees,
                    numberOfHouseHoldWithBasicNutritionSourceDomesticAnimal,
                    selectCentersCategory,
                    numberOfPreSchoolsGovernment,
                    numberOfPrimarySchoolsGovernment,
                    numberOfSecondarySchoolsGovernment,
                    numberOfUniversitiesGovernment,
                    numberOfSpecialNeedsSchoolsGovernment,
                    numberOfDispensaryGovernment,
                    numberOfHealthCentersGovernment,
                    numberOfHospitalGovernment,
                    numberOfSpecialClinicsGovernment,
                    numberOfLaboratoryGovernment,
                    numberOfPharmacyGovernment,
                    numberOfADDOGovernment,
                    numberOfMaternityHomeGovernment,
                    numberOfOrphanCareCentersGovernment,
                    numberOfCentersForChildrenWithDisabilitiesGovernment,
                    numberOfCbecdcRehabilitationCentreGovernment,
                    numberOfDayCareCentersGovernment,
                    numberOfElderlyCareCentersGovernment,
                    numberOfPreSchoolsFaithBasedOrganisation,
                    numberOfPrimarySchoolsFaithBasedOrganisation,
                    numberOfSecondarySchoolsFaithBasedOrganisation,
                    numberOfUniversitiesFaithBasedOrganisation,
                    numberOfSpecialNeedsSchoolsFaithBasedOrganisation,
                    numberOfDispensaryFaithBasedOrganisation,
                    numberOfHealthCentersFaithBasedOrganisation,
                    numberOfHospitalFaithBasedOrganisation,
                    numberOfSpecialClinicsFaithBasedOrganisation,
                    numberOfLaboratoryFaithBasedOrganisation,
                    numberOfPharmacyFaithBasedOrganisation,
                    numberOfADDOFaithBasedOrganisation,
                    numberOfMaternityHomeFaithBasedOrganisation,
                    numberOfOrphanCareCentersFaithBasedOrganisation,
                    numberOfCentersForChildrenWithDisabilitiesFaithBasedOrganisation,
                    numberOfCbecdcRehabilitationCentreFaithBasedOrganisation,
                    numberOfDayCareCentersFaithBasedOrganisation,
                    numberOfElderlyCareCentersFaithBasedOrganisation,
                    numberOfPreSchoolsPublic,
                    numberOfPrimarySchoolsPublic,
                    numberOfSecondarySchoolsPublic,
                    numberOfUniversitiesPublic,
                    numberOfSpecialNeedsSchoolsPublic,
                    numberOfDispensaryPublic,
                    numberOfHealthCentersPublic,
                    numberOfHospitalPublic,
                    numberOfSpecialClinicsPublic,
                    numberOfLaboratoryPublic,
                    numberOfPharmacyPublic,
                    numberOfADDOPublic,
                    numberOfMaternityHomePublic,
                    numberOfOrphanCareCentersPublic,
                    numberOfCentersForChildrenWithDisabilitiesPublic,
                    numberOfCbecdcRehabilitationCentrePublic,
                    numberOfDayCareCentersPublic,
                    numberOfElderlyCareCentersPublic,
                    numberOfPreSchoolsPrivate,
                    numberOfPrimarySchoolsPrivate,
                    numberOfSecondarySchoolsPrivate,
                    numberOfUniversitiesPrivate,
                    numberOfSpecialNeedsSchoolsPrivate,
                    numberOfDispensaryPrivate,
                    numberOfHealthCentersPrivate,
                    numberOfHospitalPrivate,
                    numberOfSpecialClinicsPrivate,
                    numberOfLaboratoryPrivate,
                    numberOfPharmacyPrivate,
                    numberOfADDOPrivate,
                    numberOfMaternityHomePrivate,
                    numberOfOrphanCareCentersPrivate,
                    numberOfCentersForChildrenWithDisabilitiesPrivate,
                    numberOfCbecdcRehabilitationCentrePrivate,
                    numberOfDayCareCentersPrivate,
                    numberOfElderlyCareCentersPrivate,
                    numberOfFoodShopVisited,
                    numberOfRestaurantsVisited,
                    numberOfButcheriesVisited,
                    numberOfBarAndClubsVisited,
                    numberOfGuestHouseVisited,
                    numberOfLocaFoodVendorsVisited,
                    numberOfMarketsVisited,
                    numberOfPublicToiletsVisited,
                    numberOfBusStationsVisited,
                    numberOfPrimarySchoolsVisited,
                    numberOfSecondarySchoolsVisited,
                    numberOfHospitalVisited,
                    numberOfHealthCentersVisited,
                    numberOfDispensariesVisited,
                    numberOfOfficesVisited,
                    numberOfUniversitiesCollegeVisited,
                    numberOfFoodShopThatMetTheStandards,
                    numberOfRestaurantsThatMetTheStandards,
                    numberOfButcheriesThatMetTheStandards,
                    numberOfBarAndClubsThatMetTheStandards,
                    numberOfGuestHouseThatMetTheStandards,
                    numberOfLocaFoodVendorsThatMetTheStandards,
                    numberOfMarketsThatMetTheStandards,
                    numberOfPublicToiletsThatMetTheStandards,
                    numberOfBusStationsThatMetTheStandards,
                    numberOfPrimarySchoolsThatMetTheStandards,
                    numberOfSecondarySchoolsThatMetTheStandards,
                    numberOfHospitalThatMetTheStandards,
                    numberOfHealthCentersThatMetTheStandards,
                    numberOfDispensariesThatMetTheStandards,
                    numberOfOfficesThatMetTheStandards,
                    numberOfUniversitiesCollegeThatMetTheStandards,
                    numberOfInspectedAgricultureAreas,
                    numberOfInspectedLivestockKeepingAreas,
                    numberOfInspectedFishingAreas,
                    numberOfInspectedIndustriesAreas,
                    numberOfInspectedOfficesAreas,
                    numberOfInspectedTransportationAreas,
                    numberOfOtherInspectedAreas,
                    numberOfAgricultureAreasInspectedWithRiskIndicators,
                    numberOfLivestockKeepingAreasInspectedWithRiskIndicators,
                    numberOfFishingAreasInspectedWithRiskIndicators,
                    numberOfIndustriesAreasInspectedWithRiskIndicators,
                    numberOfOfficesAreasInspectedWithRiskIndicators,
                    numberOfTransportationAreasInspectedWithRiskIndicators,
                    numberOfOtherAreasInspectedWithRiskIndicators,
                    numberOfInspectedGrains,
                    numberOfInspectedLegumes,
                    numberOfInspectedMeat,
                    numberOfInspectedFishing,
                    numberOfInspectedAlcoholicBeverages,
                    numberOfInspectedNonAlcoholicBeverages,
                    numberOfGrainsDiscarded,
                    numberOfLegumesDiscarded,
                    numberOfMeatDiscarded,
                    numberOfFishingDiscarded,
                    numberOfAlcoholicBeverageDiscarded,
                    numberOfNonAlcoholicBeverageDiscarded,
                    healthReportsAffectingPeopleInWorkplacesRespiratoryDiseases,
                    healthReportsAffectingPeopleInWorkplacesToxicChemicals,
                    healthReportsAffectingPeopleInWorkplacesBurns,
                    healthReportsAffectingPeopleInWorkplacesHearingLoss,
                    healthReportsAffectingPeopleInWorkplacesEyeProblems,
                    healthReportsAffectingPeopleInWorkplacesOtherEffects,
                    amountOfSolidWasteGeneratedAnnuallyTons,
                    amountOfSolidWasteDisposedAtADesignatedSiteAnnuallyTons,
                    numberOfWasteCollectionEquipmentVehicles,
                    numberOfWasteCollectionEquipmentTractors,
                    numberOfWasteCollectionEquipmentCarts,
                    numberOfWasteCollectionEquipmentWheelbarrows,
                    numberOfWasteCollectionEquipmentOthers,
                    numberOfAreasSprayedWithPesticidesPonds,
                    numberOfAreasSprayedWithPesticidesCans,
                    numberOfAreasSprayedWithPesticidesDrums,
                    numberOfAreasSprayedWithPesticidesBarrels,
                    numberOfAreasSprayedWithPesticidesCoconutShells,
                    numberOfTimesSprayingWasDonePonds,
                    numberOfTimesSprayingWasDoneCans,
                    numberOfTimesSprayingWasDoneDrums,
                    numberOfTimesSprayingWasDoneBarrels,
                    numberOfTimesSprayingWasDoneCoconutShells,
                    typesOfPesticidesUsedPonds,
                    typesOfPesticidesUsedCans,
                    typesOfPesticidesUsedDrums,
                    typesOfPesticidesUsedBarrels,
                    typesOfPesticidesUsedCoconutShells,
                    amountOfPesticideUsedPonds,
                    amountOfPesticideUsedCans,
                    amountOfPesticideUsedDrums,
                    amountOfPesticideUsedBarrels,
                    amountOfPesticideUsedCoconutShells,
                    numberOfHouseHolds,
                    numberOfMaleCapableOfEngagingInEconomicActivities,
                    numberOfFemaleCapableOfEngagingInEconomicActivities,
                    numberOfMaleEngagedInEconomicActivities,
                    numberOfFemaleEngagedInEconomicActivities,
                    numberOfHouseholdsMostCommonlyUseTapAsSourcesOfWater,
                    numberOfHouseholdsMostCommonlyUseRiverAsSourcesOfWater,
                    numberOfHouseholdsMostCommonlyUseShallowWellAsSourcesOfWater,
                    numberOfHouseholdsMostCommonlyUseWaterPondAsSourcesOfWater,
                    numberOfHouseholdsMostCommonlyUseSmallDamAsSourcesOfWater,
                    numberOfHouseholdsMostCommonlyUseLakeAsSourcesOfWater,
                    numberOfHouseholdsMostCommonlyUseSpringAsSourcesOfWater,
                    numberOfHouseholdsUsingElectricityAsSourceOfEnergyForLighting,
                    numberOfHouseholdsUsingSolarAsSourceOfEnergyForLighting,
                    numberOfHouseholdsUsingKerosineAsSourceOfEnergyForLighting,
                    numberOfHouseholdsUsingKoroboiAsSourceOfEnergyForLighting,
                    numberOfHouseholdsUsingOtherSourceOfEnergyForLighting,
                    numberOfHouseholdsUsingElectricityAsSourceOfCookingEnergy,
                    numberOfHouseholdsUsingSolarAsSourceOfCookingEnergy,
                    numberOfHouseholdsUsingKerosineAsSourceOfCookingEnergy,
                    numberOfHouseholdsUsingGasAsSourceOfCookingEnergy,
                    numberOfHouseholdsUsingCharcoalAsSourceOfCookingEnergy,
                    numberOfHouseholdsUsingFirewoodAsSourceOfCookingEnergy,
                    numberOfHealthCommitteeMembersForEffectiveCommitteeMeetings,
                    numberOfCommitteeMembersAttendedFisrtQuarter,
                    numberOfRegisteredAlternativeMedicineServiceProviders,
                    numberOfRegisteredTraditionalMedicineServiceProviders,
                    numberOfUnregisteredAlternativeMedicineServiceProviders,
                    numberOfUnregisteredTraditionalMedicineServiceProviders,
                    lastInteractedWith
            );
        }
    }


    private void processHtsSamplesEvent(Event event) {
        List<Obs> htsSamplesObs = event.getObs();

        String sampleType = null;
        String iqcType = null;
        String pitcTestingPoint = null;
        Long lastInteractedWith = null;

        if (!htsSamplesObs.isEmpty()) {
            for (Obs obs : htsSamplesObs) {
                if (org.smartregister.chw.hts.util.DBConstants.KEY.SAMPLE_TYPE.equals(obs.getFormSubmissionField())) {
                    sampleType = (String) obs.getValue();
                } else if (org.smartregister.chw.hts.util.DBConstants.KEY.IQC_TYPE.equals(obs.getFormSubmissionField())) {
                    iqcType = (String) obs.getValue();
                } else if (org.smartregister.chw.hts.util.DBConstants.KEY.PITC_TESTING_POINT.equals(obs.getFormSubmissionField())) {
                    pitcTestingPoint = (String) obs.getValue();
                } else if (org.smartregister.chw.hts.util.DBConstants.KEY.LAST_INTERACTED_WITH.equals(obs.getFormSubmissionField())) {
                    try {
                        lastInteractedWith = Long.parseLong((String) obs.getValue());
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }
            HtsDao.saveSampleRegistration(event.getBaseEntityId(), sampleType, iqcType, pitcTestingPoint, lastInteractedWith);
        }
    }

    private void processTbLeprosyMobilizationEvent(Event event) {
        List<Obs> mobilizationObs = event.getObs();
        String mobilizationDate = null;
        String femaleClientsReached = null;
        String maleClientsReached = null;

        if (!mobilizationObs.isEmpty()) {
            for (Obs obs : mobilizationObs) {
                if (org.smartregister.chw.tbleprosy.util.DBConstants.KEY.MOBILIZATION_DATE.equals(obs.getFormSubmissionField())) {
                    mobilizationDate = (String) obs.getValue();
                } else if (org.smartregister.chw.tbleprosy.util.DBConstants.KEY.FEMALE_CLIENTS_REACHED.equals(obs.getFormSubmissionField())) {
                    femaleClientsReached = (String) obs.getValue();
                } else if (org.smartregister.chw.tbleprosy.util.DBConstants.KEY.MALE_CLIENTS_REACHED.equals(obs.getFormSubmissionField())) {
                    maleClientsReached = (String) obs.getValue();
                }
            }
            TbLeprosyMobilizationDao.updateData(event.getBaseEntityId(), mobilizationDate, femaleClientsReached, maleClientsReached);
        }
    }

    private void processCecapMobilizationEvent(Event event) {
        List<Obs> mobilizationObs = event.getObs();
        String mobilizationDate = null;
        String femaleClientsReached = null;
        String maleClientsReached = null;
        String healthEducationProvided = null;

        if (!mobilizationObs.isEmpty()) {
            for (Obs obs : mobilizationObs) {
                if (org.smartregister.chw.hivst.util.DBConstants.KEY.MOBILIZATION_DATE.equals(obs.getFormSubmissionField())) {
                    mobilizationDate = (String) obs.getValue();
                } else if (org.smartregister.chw.hivst.util.DBConstants.KEY.FEMALE_CLIENTS_REACHED.equals(obs.getFormSubmissionField())) {
                    femaleClientsReached = (String) obs.getValue();
                } else if (org.smartregister.chw.hivst.util.DBConstants.KEY.MALE_CLIENTS_REACHED.equals(obs.getFormSubmissionField())) {
                    maleClientsReached = (String) obs.getValue();
                } else if ("health_education_provided".equals(obs.getFormSubmissionField())) {
                    healthEducationProvided = obs.getValues().toString();
                }
            }
            CecapDao.updateData(event.getBaseEntityId(), mobilizationDate, femaleClientsReached, maleClientsReached, healthEducationProvided);
        }
    }

    protected void processCDPOrderEvent(Event event) {
        List<Obs> visitObs = event.getObs();
        String condomBrand = "";
        String condomType = "";
        String receivingOrderFacility = "";
        String quantityRequested = "0";
        String requestDate = "";
        //By default the request type would be set to community_to_facility
        String requestType = org.smartregister.chw.cdp.util.Constants.ORDER_TYPES.COMMUNITY_TO_FACILITY_ORDER;
        String locationId = event.getLocationId();
        String baseEntityId = event.getBaseEntityId();
        String formSubmissionId = event.getFormSubmissionId();
        String teamId = event.getTeamId();

        if (visitObs.size() > 0) {
            for (Obs obs : visitObs) {
                if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_TYPE.equals(obs.getFieldCode())) {
                    condomType = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_BRAND.equals(obs.getFieldCode())) {
                    condomBrand = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOMS_REQUESTED.equals(obs.getFieldCode())) {
                    quantityRequested = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.RECEIVING_ORDER_FACILITY.equals(obs.getFieldCode())) {
                    receivingOrderFacility = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.REQUEST_TYPE.equals(obs.getFieldCode())) {
                    requestType = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_REQUEST_DATE.equals(obs.getFieldCode())) {
                    requestDate = (String) obs.getValue();
                }
            }
            CdpOrderDao.updateOrderData(locationId, receivingOrderFacility, baseEntityId, formSubmissionId, condomType, condomBrand, quantityRequested, requestType, requestDate, teamId);
            CdpLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());
        }
    }

    protected void processCDPOrderFeedback(Event event) {
        List<Obs> visitObs = event.getObs();
        String condomBrand = "";
        String condomType = "";
        String quantityResponse = "0";
        String requestReference = "";
        String responseStatus = "";
        String responseDate = "";
        String locationId = event.getLocationId();
        String baseEntityId = event.getBaseEntityId();

        if (visitObs.size() > 0) {
            for (Obs obs : visitObs) {
                switch (obs.getFieldCode()) {
                    case org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_TYPE:
                        condomType = (String) obs.getValue();
                        break;
                    case org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_BRAND:
                        condomBrand = (String) obs.getValue();
                        break;
                    case org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.QUANTITY_RES:
                        quantityResponse = (String) obs.getValue();
                        break;
                    case org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.REQUEST_REFERENCE:
                        requestReference = (String) obs.getValue();
                        break;
                    case org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.RESPONSE_STATUS:
                        responseStatus = (String) obs.getValue();
                        break;
                    case org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.RESPONSE_DATE:
                        responseDate = (String) obs.getValue();
                        break;
                    default:
                        break;
                }
            }
            CdpOrderDao.updateFeedbackData(locationId, baseEntityId, requestReference, condomType, condomBrand, quantityResponse, responseStatus, responseDate);
            CdpLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());
        }
    }

    private void processCDPStockChanges(Event event) {
        List<Obs> visitObs = event.getObs();
        String maleCondomsOffset = "0";
        String femaleCondomsOffset = "0";
        String restockDate = "";
        String maleCondomBrand = "";
        String femaleCondomBrand = "";
        String locationId = event.getLocationId();
        String chwName = event.getProviderId();
        String stockEventType = "";
        String issuingOrganization = "";

        if (visitObs.size() > 0) {
            for (Obs obs : visitObs) {
                if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.FEMALE_CONDOMS_OFFSET.equals(obs.getFieldCode())) {
                    femaleCondomsOffset = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.MALE_CONDOMS_OFFSET.equals(obs.getFieldCode())) {
                    maleCondomsOffset = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_RESTOCK_DATE.equals(obs.getFieldCode())) {
                    restockDate = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.STOCK_EVENT_TYPE.equals(obs.getFieldCode())) {
                    stockEventType = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.ISSUING_ORGANIZATION.equals(obs.getFieldCode())) {
                    issuingOrganization = (String) obs.getValue();
                } else if (obs.getFieldCode().equals(org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.MALE_CONDOM_BRAND)) {
                    maleCondomBrand = (String) obs.getValue();
                } else if (obs.getFieldCode().equals(org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.FEMALE_CONDOM_BRAND)) {
                    femaleCondomBrand = (String) obs.getValue();
                }
            }


            CdpStockingDao.updateStockLogData(locationId, event.getFormSubmissionId(), chwName, maleCondomBrand, femaleCondomBrand, maleCondomsOffset, femaleCondomsOffset, stockEventType, issuingOrganization, event.getEventType(), restockDate);
            CdpStockingDao.updateStockCountData(locationId, event.getFormSubmissionId(), chwName, maleCondomsOffset, femaleCondomsOffset, stockEventType, restockDate);


            completeProcessing(event);
            CdpLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());

            if (event.getEventType().equals(org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_RESTOCK)) {
                processCDPOutletStockChanges(event);
            }
        }
    }

    private void processCDPOutletStockChanges(Event event) {
        List<Obs> visitObs = event.getObs();
        String maleCondomsOffset = "0";
        String femaleCondomsOffset = "0";
        String restockDate = "";
        String baseEntityId = event.getBaseEntityId();
        String stockEventType = "";

        if (visitObs.size() > 0) {
            for (Obs obs : visitObs) {
                if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.FEMALE_CONDOMS_OFFSET.equals(obs.getFieldCode())) {
                    femaleCondomsOffset = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.MALE_CONDOMS_OFFSET.equals(obs.getFieldCode())) {
                    maleCondomsOffset = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.CONDOM_RESTOCK_DATE.equals(obs.getFieldCode())) {
                    restockDate = (String) obs.getValue();
                } else if (org.smartregister.chw.cdp.util.Constants.JSON_FORM_KEY.STOCK_EVENT_TYPE.equals(obs.getFieldCode())) {
                    stockEventType = (String) obs.getValue();
                    if (stockEventType.equals(org.smartregister.chw.cdp.util.Constants.STOCK_EVENT_TYPES.DECREMENT))
                        stockEventType = org.smartregister.chw.cdp.util.Constants.STOCK_EVENT_TYPES.INCREMENT;
                    else
                        stockEventType = org.smartregister.chw.cdp.util.Constants.STOCK_EVENT_TYPES.DECREMENT;
                }
            }
            CdpStockingDao.updateOutletStockCountData(baseEntityId, event.getFormSubmissionId(), maleCondomsOffset, femaleCondomsOffset, stockEventType, restockDate);
            completeProcessing(event);
            CdpLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());
        }
    }


    protected void processCreateManifestEvent(Event event) {
        List<Obs> visitObs = event.getObs();
        String batchNumber = "";
        String manifestType = "";
        String destinationHub = "";
        String samplesList = "";

        if (!visitObs.isEmpty()) {
            for (Obs obs : visitObs) {
                if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.BATCH_NUMBER.equals(obs.getFieldCode())) {
                    batchNumber = (String) obs.getValue();
                } else if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.MANIFEST_TYPE.equals(obs.getFieldCode())) {
                    manifestType = (String) obs.getValue();
                } else if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.DESTINATION_HUB_NAME.equals(obs.getFieldCode())) {
                    destinationHub = (String) obs.getValue();
                } else if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.SAMPLES_LIST.equals(obs.getFieldCode())) {
                    samplesList = (String) obs.getValue();
                }
            }
            LabDao.insertManifest(batchNumber, manifestType, destinationHub, samplesList, String.valueOf(event.getVersion()));
            LabLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());
        }
    }


    protected void processDispatchManifestEvent(Event event) {
        List<Obs> visitObs = event.getObs();
        String batchNumber = "";
        String dispatchDate = "";
        String dispatchTime = "";
        String dispatcherName = "";

        if (visitObs.size() > 0) {
            for (Obs obs : visitObs) {
                if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.BATCH_NUMBER.equals(obs.getFieldCode())) {
                    batchNumber = (String) obs.getValue();
                } else if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.DISPATCH_DATE.equals(obs.getFieldCode())) {
                    dispatchDate = (String) obs.getValue();
                } else if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.DISPATCH_TIME.equals(obs.getFieldCode())) {
                    dispatchTime = (String) obs.getValue();
                } else if (org.smartregister.chw.lab.util.Constants.JSON_FORM_KEY.DISPATCHER_NAME.equals(obs.getFieldCode())) {
                    dispatcherName = (String) obs.getValue();
                }
            }
            LabDao.updateManifest(batchNumber, dispatchDate, dispatchTime, dispatcherName);
            LabLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());
        }
    }


    protected void processLabSettingsEvent(Event event) {
        List<Obs> visitObs = event.getObs();
        String destinationHubName = "";
        String locationId = "";

        if (!visitObs.isEmpty()) {
            for (Obs obs : visitObs) {
                if ("name_of_hf".equals(obs.getFieldCode())) {
                    destinationHubName = (String) obs.getHumanReadableValue();
                }
                if ("location_id".equals(obs.getFieldCode())) {
                    locationId = (String) obs.getValue();
                }
            }

            if (locationId != null) {
                LabDao.saveDestinationHub(destinationHubName, locationId);
                LabLibrary.getInstance().context().getEventClientRepository().markEventAsProcessed(event.getFormSubmissionId());
            }
        }
    }

    private void clientProcessByObs(EventClient eventClient, ClientClassification clientClassification, Event event, String formSubmissionField, String humanReadableValues) {
        if (eventClient.getClient() == null) {
            return;
        }
        try {
            processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
            List<Obs> observations = event.getObs();
            for (Obs obs : observations) {
                if (obs.getFormSubmissionField().equals(formSubmissionField) && !obs.getHumanReadableValues().get(0).equals(humanReadableValues)) {
                    if (event.getEventType().equals(Constants.EVENT_TYPE.MALARIA_FOLLOW_UP_VISIT)) {
                        org.smartregister.util.Utils.startAsyncTask(new MalariaUtil.CloseMalariaMemberFromRegister(event.getBaseEntityId()), null);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Timber.d(e);
        }
    }

    // possible to delegate
    private Boolean processVaccine(EventClient vaccine, Table vaccineTable, boolean outOfCatchment) {

        try {
            if (vaccine == null || vaccine.getEvent() == null) {
                return false;
            }

            if (vaccineTable == null) {
                return false;
            }

            Timber.d("Starting processVaccine table: %s", vaccineTable.name);

            ContentValues contentValues = processCaseModel(vaccine, vaccineTable);

            // updateFamilyRelations the values to db
            if (contentValues != null && contentValues.size() > 0) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(contentValues.getAsString(VaccineRepository.DATE));

                VaccineRepository vaccineRepository = CoreChwApplication.getInstance().vaccineRepository();
                Vaccine vaccineObj = new Vaccine();
                vaccineObj.setBaseEntityId(contentValues.getAsString(VaccineRepository.BASE_ENTITY_ID));
                vaccineObj.setName(contentValues.getAsString(VaccineRepository.NAME));
                if (contentValues.containsKey(VaccineRepository.CALCULATION)) {
                    vaccineObj.setCalculation(parseInt(contentValues.getAsString(VaccineRepository.CALCULATION)));
                }
                vaccineObj.setDate(date);
                vaccineObj.setAnmId(contentValues.getAsString(VaccineRepository.ANMID));
                vaccineObj.setLocationId(contentValues.getAsString(VaccineRepository.LOCATION_ID));
                vaccineObj.setSyncStatus(VaccineRepository.TYPE_Synced);
                vaccineObj.setFormSubmissionId(vaccine.getEvent().getFormSubmissionId());
                vaccineObj.setEventId(vaccine.getEvent().getEventId());
                vaccineObj.setOutOfCatchment(outOfCatchment ? 1 : 0);
                vaccineObj.setProgramClientId(getVaccineProgramClient(vaccine));

                String createdAtString = contentValues.getAsString(VaccineRepository.CREATED_AT);
                Date createdAt = getDate(createdAtString);
                vaccineObj.setCreatedAt(createdAt);

                addVaccine(vaccineRepository, vaccineObj);

                Timber.d("Ending processVaccine table: %s", vaccineTable.name);
            }
            return true;

        } catch (Exception e) {

            Timber.e(e, "Process Vaccine Error");
            return null;
        }
    }

    private String getVaccineProgramClient(EventClient eventClient) {
        Map<String, String> details = eventClient.getEvent().getDetails();
        return details != null ? details.get(IMConstants.VaccineEvent.PROGRAM_CLIENT_ID) : null;
    }

    // possible to delegate
    protected Boolean processService(EventClient service, Table serviceTable) {

        try {

            if (service == null || service.getEvent() == null) {
                return false;
            }

            if (serviceTable == null) {
                return false;
            }

            Timber.d("Starting processService table: %s", serviceTable.name);

            ContentValues contentValues = processCaseModel(service, serviceTable);

            // updateFamilyRelations the values to db
            if (contentValues != null && contentValues.size() > 0) {
                String name = contentValues.getAsString(RecurringServiceTypeRepository.NAME);

                if (StringUtils.isNotBlank(name)) {
                    name = name.replaceAll("_", " ").replace("dose", "").trim();
                }


                String eventDateStr = contentValues.getAsString(RecurringServiceRecordRepository.DATE);
                Date date = getDate(eventDateStr);
                String value = null;

                if (StringUtils.containsIgnoreCase(name, "Exclusive breastfeeding")) {
                    value = contentValues.getAsString(RecurringServiceRecordRepository.VALUE);
                }

                RecurringServiceTypeRepository recurringServiceTypeRepository = ImmunizationLibrary.getInstance().recurringServiceTypeRepository();
                List<ServiceType> serviceTypeList = recurringServiceTypeRepository.searchByName(name);
                if (serviceTypeList == null || serviceTypeList.isEmpty()) {
                    return false;
                }

                if (date == null) {
                    return false;
                }

                RecurringServiceRecordRepository recurringServiceRecordRepository = ImmunizationLibrary.getInstance().recurringServiceRecordRepository();
                ServiceRecord serviceObj = new ServiceRecord();
                serviceObj.setBaseEntityId(contentValues.getAsString(RecurringServiceRecordRepository.BASE_ENTITY_ID));
                serviceObj.setName(name);
                serviceObj.setDate(date);
                serviceObj.setAnmId(contentValues.getAsString(RecurringServiceRecordRepository.ANMID));
                serviceObj.setLocationId(contentValues.getAsString(RecurringServiceRecordRepository.LOCATION_ID));
                serviceObj.setSyncStatus(RecurringServiceRecordRepository.TYPE_Synced);
                serviceObj.setFormSubmissionId(service.getEvent().getFormSubmissionId());
                serviceObj.setEventId(service.getEvent().getEventId()); //FIXME hard coded id
                serviceObj.setValue(value);
                serviceObj.setRecurringServiceId(serviceTypeList.get(0).getId());

                String createdAtString = contentValues.getAsString(RecurringServiceRecordRepository.CREATED_AT);
                Date createdAt = getDate(createdAtString);
                serviceObj.setCreatedAt(createdAt);

                recurringServiceRecordRepository.add(serviceObj);

                Timber.d("Ending processService table: %s", serviceTable.name);
            }
            return true;

        } catch (Exception e) {
            Timber.e(e, "Process Service Error");
            return null;
        }
    }

    private void processVisitEvent(List<EventClient> eventClients, String parentEventName) {
        for (EventClient eventClient : eventClients) {
            processVisitEvent(eventClient, parentEventName); // save locally
        }
    }

    // possible to delegate
    private void processVisitEvent(EventClient eventClient) {
        try {
            NCUtils.processHomeVisit(eventClient); // save locally
        } catch (Exception e) {
            String formID = (eventClient != null && eventClient.getEvent() != null) ? eventClient.getEvent().getFormSubmissionId() : "no form id";
            Timber.e("Form id " + formID + ". " + e.toString());
        }
    }

    private void processVisitEvent(EventClient eventClient, String parentEventName) {
        try {
            NCUtils.processSubHomeVisit(eventClient, parentEventName); // save locally
        } catch (Exception e) {
            String formID = (eventClient != null && eventClient.getEvent() != null) ? eventClient.getEvent().getFormSubmissionId() : "no form id";
            Timber.e("Form id " + formID + ". " + e.toString());
        }
    }

    /**
     * Update the family members
     *
     * @param familyID
     */
    private void processRemoveFamily(String familyID, Date eventDate) {

        Date myEventDate = eventDate;
        if (myEventDate == null) {
            myEventDate = new Date();
        }

        if (familyID == null) {
            return;
        }

        AllCommonsRepository commonsRepository = CoreChwApplication.getInstance().getAllCommonsRepository(CoreConstants.TABLE_NAME.FAMILY);
        if (commonsRepository != null) {

            ContentValues values = new ContentValues();
            values.put(DBConstants.KEY.DATE_REMOVED, new SimpleDateFormat("yyyy-MM-dd").format(myEventDate));
            values.put("is_closed", 1);

            CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CoreConstants.TABLE_NAME.FAMILY, values, DBConstants.KEY.BASE_ENTITY_ID + " = ?  ", new String[]{familyID});

            CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CoreConstants.TABLE_NAME.CHILD, values, DBConstants.KEY.RELATIONAL_ID + " = ?  ", new String[]{familyID});

            CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CoreConstants.TABLE_NAME.FAMILY_MEMBER, values, DBConstants.KEY.RELATIONAL_ID + " = ?  ", new String[]{familyID});

            // clean fts table
            CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CommonFtsObject.searchTableName(CoreConstants.TABLE_NAME.FAMILY), values, CommonFtsObject.idColumn + " = ?  ", new String[]{familyID});

            CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CommonFtsObject.searchTableName(CoreConstants.TABLE_NAME.CHILD), values, String.format(" %s in (select base_entity_id from %s where relational_id = ? )  ", CommonFtsObject.idColumn, CoreConstants.TABLE_NAME.CHILD), new String[]{familyID});

            CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CommonFtsObject.searchTableName(CoreConstants.TABLE_NAME.FAMILY_MEMBER), values, String.format(" %s in (select base_entity_id from %s where relational_id = ? )  ", CommonFtsObject.idColumn, CoreConstants.TABLE_NAME.FAMILY_MEMBER), new String[]{familyID});

            List<String> familyMembers = ChildDao.getFamilyMembers(familyID);
            for (String baseEntityId : familyMembers) {
                CoreChwApplication.getInstance().getContext().alertService().deleteOfflineAlerts(baseEntityId);
            }
        }
    }

    private SQLiteDatabase getWritableDatabase() {
        return CoreChwApplication.getInstance().getRepository().getWritableDatabase();
    }

    private Map<String, String> readObs(Event event) {
        Map<String, String> obsMap = new HashMap<>();
        if (event.getObs() != null) {
            for (Obs obs : event.getObs()) {
                if (obs.getValues().size() > 0) {
                    Object object = obs.getValues().get(0);
                    obsMap.put(obs.getFormSubmissionField(), (object == null) ? null : object.toString());
                }
            }
        }
        return obsMap;
    }

    private Date getDate(Map<String, String> obsMap, String key) throws ParseException {
        String strDod = obsMap.get(key);
        if (StringUtils.isBlank(strDod)) return null;

        SimpleDateFormat nfDf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        return nfDf.parse(strDod);
    }

    protected void processRemoveMember(String baseEntityId, Event event) {

        Date myEventDate = event.getEventDate().toDate();
        if (myEventDate == null) {
            myEventDate = new Date();
        }

        if (baseEntityId == null) {
            return;
        }

        SimpleDateFormat defaultDf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Map<String, String> obsMap = readObs(event);

        AllCommonsRepository commonsRepository = CoreChwApplication.getInstance().getAllCommonsRepository(CoreConstants.TABLE_NAME.FAMILY_MEMBER);
        if (commonsRepository != null) {

            ContentValues values = new ContentValues();
            values.put(DBConstants.KEY.DATE_REMOVED, defaultDf.format(myEventDate));
            values.put("is_closed", 1);

            // clean fts table
            getWritableDatabase().update(CommonFtsObject.searchTableName(CoreConstants.TABLE_NAME.FAMILY_MEMBER), values, " object_id  = ?  ", new String[]{baseEntityId});


            try {
                Date dod = getDate(obsMap, "date_died");
                if (dod != null) values.put(DBConstants.KEY.DOD, defaultDf.format(dod));
            } catch (ParseException e) {
                Timber.e(e);
            }

            getWritableDatabase().update(CoreConstants.TABLE_NAME.FAMILY_MEMBER, values, DBConstants.KEY.BASE_ENTITY_ID + " = ?  ", new String[]{baseEntityId});

            // Utils.context().commonrepository(CoreConstants.TABLE_NAME.FAMILY_MEMBER).populateSearchValues(baseEntityId, DBConstants.KEY.DATE_REMOVED, new SimpleDateFormat("yyyy-MM-dd").format(eventDate), null);
            //CoreChwApplication.getInstance().getContext().alertService().deleteOfflineAlerts(baseEntityId);
        }
    }

    protected void processRemoveChild(String baseEntityId, Event event) {

        Date myEventDate = event.getEventDate().toDate();
        if (myEventDate == null) {
            myEventDate = new Date();
        }

        if (baseEntityId == null) {
            return;
        }

        Map<String, String> obsMap = readObs(event);
        SimpleDateFormat defaultDf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        AllCommonsRepository commonsRepository = CoreChwApplication.getInstance().getAllCommonsRepository(CoreConstants.TABLE_NAME.CHILD);
        if (commonsRepository != null) {

            ContentValues values = new ContentValues();
            values.put(DBConstants.KEY.DATE_REMOVED, defaultDf.format(myEventDate));
            values.put("is_closed", 1);

            // clean fts table
            getWritableDatabase().update(CommonFtsObject.searchTableName(CoreConstants.TABLE_NAME.CHILD), values, CommonFtsObject.idColumn + "  = ?  ", new String[]{baseEntityId});

            try {
                Date dod = getDate(obsMap, "date_died");
                if (dod != null) values.put(DBConstants.KEY.DOD, defaultDf.format(dod));
            } catch (ParseException e) {
                Timber.e(e);
            }

            getWritableDatabase().update(CoreConstants.TABLE_NAME.CHILD, values, DBConstants.KEY.BASE_ENTITY_ID + " = ?  ", new String[]{baseEntityId});

            // Utils.context().commonrepository(CoreConstants.TABLE_NAME.CHILD).populateSearchValues(baseEntityId, DBConstants.KEY.DATE_REMOVED, new SimpleDateFormat("yyyy-MM-dd").format(eventDate), null);
            //CoreChwApplication.getInstance().getContext().alertService().deleteOfflineAlerts(baseEntityId);
        }
    }

    private ContentValues processCaseModel(EventClient eventClient, Table table) {
        try {
            List<Column> columns = table.columns;
            ContentValues contentValues = new ContentValues();

            for (Column column : columns) {
                processCaseModel(eventClient.getEvent(), eventClient.getClient(), column, contentValues);
            }

            return contentValues;
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private Integer parseInt(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
        return null;
    }

    private Date getDate(String eventDateStr) {
        Date date = null;
        if (StringUtils.isNotBlank(eventDateStr)) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
                date = dateFormat.parse(eventDateStr);
            } catch (ParseException e) {
                try {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    date = dateFormat.parse(eventDateStr);
                } catch (ParseException pe) {
                    try {
                        date = DateUtil.parseDate(eventDateStr);
                    } catch (ParseException pee) {
                        Timber.e(pee, pee.toString());
                    }
                }
            }
        }
        return date;
    }

    @Override
    public void updateClientDetailsTable(Event event, Client client) {
        Timber.d("Started updateClientDetailsTable");
        event.addDetails("detailsUpdated", Boolean.TRUE.toString());
        Timber.d("Finished updateClientDetailsTable");
    }

    private void processVisitEvent(List<EventClient> eventClients) {
        for (EventClient eventClient : eventClients) {
            processVisitEvent(eventClient); // save locally
        }
    }

    private Float parseFloat(String string) {
        try {
            return Float.valueOf(string);
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
        return null;
    }

    private CommunityResponderModel getCommunityResponderFromObs(Event event) {
        List<Obs> responderObs = event.getObs();
        CommunityResponderModel communityResponderModel = new CommunityResponderModel();
        for (Obs obs : responderObs) {
            if (obs.getFormSubmissionField().equals(CoreConstants.JsonAssets.RESPONDER_NAME)) {
                String value = StockUsageReportUtils.getObsValue(obs);
                if (StringUtils.isNotBlank(value)) {
                    communityResponderModel.setResponderName(value);
                    continue;
                } else return null;
            } else if (obs.getFormSubmissionField().equals(CoreConstants.JsonAssets.RESPONDER_PHONE_NUMBER)) {
                String value = StockUsageReportUtils.getObsValue(obs);
                if (StringUtils.isNotBlank(value)) {
                    communityResponderModel.setResponderPhoneNumber(value);
                    continue;
                } else return null;
            } else if (obs.getFormSubmissionField().equals(CoreConstants.JsonAssets.RESPONDER_ID)) {
                String value = StockUsageReportUtils.getObsValue(obs);
                if (StringUtils.isNotBlank(value)) {
                    communityResponderModel.setId(value);
                } else {
                    communityResponderModel.setId(event.getBaseEntityId());
                    continue;
                }
            } else if (obs.getFormSubmissionField().equals(CoreConstants.JsonAssets.RESPONDER_GPS)) {
                String value = StockUsageReportUtils.getObsValue(obs);
                if (StringUtils.isNotBlank(value)) {
                    communityResponderModel.setResponderLocation(value);
                    continue;
                } else return null;
            }
        }
        return communityResponderModel;
    }

    private void clientProcessCommunityResponderEvent(Event event) {
        CommunityResponderModel communityResponderModel = getCommunityResponderFromObs(event);
        if (communityResponderModel != null) {
            CommunityResponderRepository repo = CoreChwApplication.getInstance().communityResponderRepository();
            if (StringUtils.isBlank(communityResponderModel.getId()))
                communityResponderModel.setId(event.getBaseEntityId());
            repo.addOrUpdate(communityResponderModel);
        }
    }
}
