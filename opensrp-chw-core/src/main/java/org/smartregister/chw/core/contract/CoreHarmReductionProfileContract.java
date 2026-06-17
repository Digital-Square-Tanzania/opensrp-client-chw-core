package org.smartregister.chw.core.contract;

import org.json.JSONObject;
import org.smartregister.chw.harmreduction.contract.HarmReductionProfileContract;

public class CoreHarmReductionProfileContract {
    public interface View extends HarmReductionProfileContract.View {
        void startFormActivity(JSONObject formJson);
    }

    public interface Presenter extends HarmReductionProfileContract.Presenter {
    }

    public interface Interactor extends HarmReductionProfileContract.Interactor {
    }
}
