package org.smartregister.chw.core.contract;

import org.json.JSONObject;
import org.smartregister.chw.vmmc.contract.VmmcProfileContract;

public class CoreVmmcProfileContract {
    public interface View extends VmmcProfileContract.View {
        void startFormActivity(JSONObject formJson);
    }

    public interface Presenter extends VmmcProfileContract.Presenter {


    }

    public interface Interactor extends VmmcProfileContract.Interactor {

    }
}
