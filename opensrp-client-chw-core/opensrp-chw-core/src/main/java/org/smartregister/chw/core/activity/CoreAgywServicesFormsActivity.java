package org.smartregister.chw.core.activity;

import android.content.Intent;

import org.json.JSONObject;
import org.smartregister.chw.agyw.activity.BaseServicesFormActivity;
import org.smartregister.family.util.JsonFormUtils;

public class CoreAgywServicesFormsActivity extends BaseServicesFormActivity {
    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }
}
