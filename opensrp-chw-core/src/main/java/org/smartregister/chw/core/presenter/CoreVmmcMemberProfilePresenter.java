package org.smartregister.chw.core.presenter;

import org.smartregister.chw.core.contract.CoreVmmcProfileContract;

import org.smartregister.chw.vmmc.domain.MemberObject;
import org.smartregister.chw.vmmc.presenter.BaseVmmcProfilePresenter;

public class CoreVmmcMemberProfilePresenter extends BaseVmmcProfilePresenter implements CoreVmmcProfileContract.Presenter {

    public CoreVmmcMemberProfilePresenter(CoreVmmcProfileContract.View view, CoreVmmcProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
        this.interactor = interactor;
    }

    @Override
    public CoreVmmcProfileContract.View getView() {
        if (view != null) {
            return (CoreVmmcProfileContract.View) view.get();
        }
        return null;
    }

}
