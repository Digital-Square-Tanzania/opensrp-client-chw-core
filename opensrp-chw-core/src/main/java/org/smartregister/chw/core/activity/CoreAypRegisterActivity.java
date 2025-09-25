package org.smartregister.chw.core.activity;

import android.os.Bundle;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.core.fragment.CoreAypRegisterFragment;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.ayp.activity.BaseAypRegisterActivity;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class CoreAypRegisterActivity extends BaseAypRegisterActivity {
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
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.AYP_IN_SCHOOL);
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.AYP_OUT_SCHOOL);
        }
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new CoreAypRegisterFragment();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        if (getFormConfig() != null) {
            try {
                getIntent().putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
            } catch (Exception ignored) {
            }
        }
        super.startFormActivity(jsonForm);
    }
}

