package org.smartregister.chw.core.presenter;

import org.smartregister.chw.core.contract.CoreHarmReductionProfileContract;
import org.smartregister.chw.harmreduction.domain.MemberObject;
import org.smartregister.chw.harmreduction.presenter.BaseHarmReductionProfilePresenter;

public class CoreHarmReductionMemberProfilePresenter extends BaseHarmReductionProfilePresenter implements CoreHarmReductionProfileContract.Presenter {

    public CoreHarmReductionMemberProfilePresenter(CoreHarmReductionProfileContract.View view, CoreHarmReductionProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
        this.interactor = interactor;
    }

    @Override
    public CoreHarmReductionProfileContract.View getView() {
        if (view != null) {
            return (CoreHarmReductionProfileContract.View) view.get();
        }
        return null;
    }
}
