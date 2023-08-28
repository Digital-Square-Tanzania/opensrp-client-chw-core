package org.smartregister.chw.core.activity;

import android.os.Bundle;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.presenter.CoreHivstRegisterPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hivst.activity.BaseHivstRegisterActivity;
import org.smartregister.chw.hivst.interactor.BaseHivstRegisterInteractor;
import org.smartregister.chw.hivst.model.BaseHivstRegisterModel;
import org.smartregister.family.util.JsonFormUtils;

public class CoreHivstRegisterActivity extends BaseHivstRegisterActivity {
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
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.HIV_SELF_TESTING);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        startActivityForResult(FormUtils.getStartFormActivity(jsonForm, this.getString(R.string.hivst), this), JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void initializePresenter() {
        presenter = new CoreHivstRegisterPresenter(this, new BaseHivstRegisterModel(), new BaseHivstRegisterInteractor());
    }
}
