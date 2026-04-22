package org.smartregister.chw.core.activity;

import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;

public class CoreAypFacilityServicesRegisterActivity extends CoreAypRegisterActivity {

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.AYP_FACILITY);
        }
    }
}
