package org.smartregister.chw.core.contract;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyProfileMemberContract;
import org.smartregister.family.domain.FamilyEventClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface FamilyRemoveMemberContract {

    interface Presenter extends FamilyProfileMemberContract.Presenter {

        void removeMember(CommonPersonObjectClient client);

        void processMember(Map<String, String> familyDetails, CommonPersonObjectClient client);

        void removeEveryone(String familyName, String details);

        void onFamilyRemoved(Boolean success);

        void processRemoveForm(JSONObject jsonObject);

        void memberRemoved(String removalType);

        void saveFamilyRegistrationOnMemberRemoval(String jsonString, boolean isEditMode, String reasonForRemove);

        void startForm(String formName, String entityId, String baseEntityId, String metadata,
                       String currentLocationId, String reasonForRemove) throws Exception;

        boolean isEligibleForRemoval(CommonPersonObjectClient client, String removeReason);

    }

    interface View extends FamilyProfileMemberContract.View {
        void removeMember(CommonPersonObjectClient client);

        void displayChangeFamilyHeadDialog(CommonPersonObjectClient client, String familyHeadID);

        void displayChangeCareGiverDialog(CommonPersonObjectClient client, String careGiverID);

        void closeFamily(String familyName, String details);

        void goToPrevious();

        void startJsonActivity(JSONObject form);

        void onMemberRemoved(String removalType);

        void onEveryoneRemoved();

        void startJsonRegistrationFrom(JSONObject form, String reasonForRemove);

    }

    interface Interactor {

        void removeMember(String familyID, String lastLocationId, JSONObject exitForm, Presenter presenter);

        void processFamilyMember(String familyID, CommonPersonObjectClient client, Presenter presenter);

        void getFamilySummary(String familyID, InteractorCallback<HashMap<String, String>> callback);
        void saveNewFamilyRegistration(final List<FamilyEventClient> familyEventClientList,
                              final String jsonString, final boolean isEditMode, final String reasonForRemove,
                              final InteractorCallback<HashMap<String, String>> callback);

    }

    interface Model extends FamilyProfileMemberContract.Model {

        JSONObject prepareJsonForm(CommonPersonObjectClient client, String formType);

        String getForm(CommonPersonObjectClient client);

        JSONObject prepareFamilyRemovalForm(String familyID, String familyName, String details);

        List<FamilyEventClient> processFamilyMemberRemoval(String jsonString);

        JSONObject getFormAsJson(String formName, String entityId,
                                 String currentLocationId, String baseEntityId) throws Exception;
        String getLocationId(String locationName);

    }

    interface InteractorCallback<T> {
        void onResult(T result);

        void onError(Exception e);

        void onNewFamilyRegistrationSaved(String clientBaseEntityId, String familyBaseEntityId, String reasonForRemove);

        void onUniqueIdFetched(Triple<String, String, String> triple, String entityId);
    }

}
