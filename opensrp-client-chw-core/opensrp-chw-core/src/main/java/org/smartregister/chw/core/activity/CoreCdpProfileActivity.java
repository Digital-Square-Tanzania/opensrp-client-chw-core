package org.smartregister.chw.core.activity;

import android.content.Intent;

import org.json.JSONObject;
import org.smartregister.chw.cdp.activity.BaseCdpProfileActivity;
import org.smartregister.chw.cdp.contract.BaseCdpProfileContract;
import org.smartregister.family.util.JsonFormUtils;

/**
 * Created by Billy on 26/08/2022.
 */
public class CoreCdpProfileActivity extends BaseCdpProfileActivity implements BaseCdpProfileContract.View {

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

}
