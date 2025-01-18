package org.smartregister.chw.core.presenter;

import org.smartregister.chw.core.contract.CoreTbLeprosyProfileContract;
import org.smartregister.chw.tbleprosy.presenter.BaseTbLeprosyProfilePresenter;
import org.smartregister.chw.tbleprosy.domain.MemberObject;

public class CoreTbLeprosyMemberProfilePresenter extends BaseTbLeprosyProfilePresenter implements CoreTbLeprosyProfileContract.Presenter {

    public CoreTbLeprosyMemberProfilePresenter(CoreTbLeprosyProfileContract.View view, CoreTbLeprosyProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
        this.interactor = interactor;
    }

    @Override
    public CoreTbLeprosyProfileContract.View getView() {
        if (view != null) {
            return (CoreTbLeprosyProfileContract.View) view.get();
        }
        return null;
    }

}
