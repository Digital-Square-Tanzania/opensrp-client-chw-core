package org.smartregister.chw.core.presenter;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.hivst.contract.HivstRegisterContract;
import org.smartregister.chw.hivst.presenter.BaseHivstRegisterPresenter;

import timber.log.Timber;

public class CoreHivstRegisterPresenter extends BaseHivstRegisterPresenter {
    public CoreHivstRegisterPresenter(HivstRegisterContract.View view, HivstRegisterContract.Model model, HivstRegisterContract.Interactor interactor) {
        super(view, model, interactor);
    }

    @Override
    public void startForm(String formName, String entityId, String metadata, String currentLocationId, String gender, int age) throws Exception {
        if (StringUtils.isBlank(entityId)) {
            return;
        }

        JSONObject form = model.getFormAsJson(formName, entityId, currentLocationId, gender, age);
        try {
            HivMemberObject hivMemberObject = HivDao.getMember(entityId);
            if(hivMemberObject.getClientHivStatusAfterTesting().equalsIgnoreCase("positive")){
                JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
                JSONObject clientTestingHistory = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "client_testing_history");
                clientTestingHistory.put("type","hidden");
                clientTestingHistory.put("value","known_positive");
            }
        }catch (Exception e){
            Timber.e(e);
        }
        getView().startFormActivity(form);
    }
}
