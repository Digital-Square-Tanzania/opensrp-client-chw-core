package org.smartregister.chw.core.contract;

import org.json.JSONObject;
import org.smartregister.chw.hivst.contract.HivstProfileContract;
import org.smartregister.repository.AllSharedPreferences;

public class CoreHivstProfileContract {
    public interface View extends HivstProfileContract.View {
        void startFormActivity(JSONObject formJson);
    }

    public interface Presenter extends HivstProfileContract.Presenter {
        void startHivstFollowupForm();

        void createHivstFollowupEvent(AllSharedPreferences allSharedPreferences, String jsonString, String entityID) throws Exception;
    }

    public interface Interactor extends HivstProfileContract.Interactor {
        void createHivstFollowupEvent(AllSharedPreferences allSharedPreferences, String jsonString, String entityIDd) throws Exception;
    }
}
