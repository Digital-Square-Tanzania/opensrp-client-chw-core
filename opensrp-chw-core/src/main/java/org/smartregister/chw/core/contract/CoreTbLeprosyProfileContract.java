package org.smartregister.chw.core.contract;

import org.json.JSONObject;
import org.smartregister.chw.tbleprosy.contract.TbLeprosyProfileContract;
import org.smartregister.chw.tbleprosy.domain.MemberObject;

public class CoreTbLeprosyProfileContract {
    public interface View extends TbLeprosyProfileContract.View {
        void startFormActivity(JSONObject formJson, MemberObject tbMemberObject);
    }

    public interface Presenter extends TbLeprosyProfileContract.Presenter {


    }

    public interface Interactor extends TbLeprosyProfileContract.Interactor {

    }
}
