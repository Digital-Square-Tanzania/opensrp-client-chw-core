package org.smartregister.chw.core.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.fragment.CoreHtsRegisterFragment;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hts.activity.BaseHtsRegisterActivity;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

/**
 * Created by ilakozejumanne@gmail.com on 17/12/2024.
 */
public class CoreHtsRegisterActivity extends BaseHtsRegisterActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationMenu.getInstance(this, null, null);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.HTS);
        }
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new CoreHtsRegisterFragment() {
            @Override
            protected void startSampleRegistration(View view) {
                //To be implemented
            }
        };
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = FormUtils.getStartFormActivity(jsonForm, null, this);
        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }
}
