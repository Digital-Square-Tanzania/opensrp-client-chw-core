package org.smartregister.chw.core.presenter;

import org.smartregister.chw.core.R;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.vmmc.contract.VmmcRegisterFragmentContract;
import org.smartregister.chw.vmmc.presenter.BaseVmmcRegisterFragmentPresenter;

public class CoreVmmcRegisterFragmentPresenter extends BaseVmmcRegisterFragmentPresenter {

    public CoreVmmcRegisterFragmentPresenter(VmmcRegisterFragmentContract.View view,
                                             VmmcRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public void processViewConfigurations() {
        super.processViewConfigurations();
        if (config.getSearchBarText() != null && getView() != null) {
            getView().updateSearchBarHint(getView().getContext().getString(R.string.search_name_or_id));
        }
    }

    @Override
    public String getMainTable() {
        return CoreConstants.TABLE_NAME.VMMC_CONFIRMATION;
    }
}
