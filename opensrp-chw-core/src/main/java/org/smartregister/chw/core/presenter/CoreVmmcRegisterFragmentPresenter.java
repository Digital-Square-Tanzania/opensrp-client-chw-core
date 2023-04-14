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

//    @Override
//    public String getMainCondition() {
//        return " ec_family_member.date_removed is null AND ec_vmmc_confirmation.vmmc  = 1 " +
//                "AND datetime('NOW') <= datetime(ec_vmmc_confirmation.last_interacted_with/1000, 'unixepoch', 'localtime','+15 days') AND ec_vmmc_confirmation.is_closed = 0";
//
//    }

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
