package org.smartregister.chw.core.presenter;

import com.vijay.jsonwizard.utils.FormUtils;

import org.smartregister.chw.core.activity.CoreTbLeprosyProfileActivity;
import org.smartregister.chw.core.activity.CoreTbProfileActivity;
import org.smartregister.chw.core.contract.CoreTbLeprosyProfileContract;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.tb.domain.TbMemberObject;
import org.smartregister.chw.tbleprosy.presenter.BaseTbLeprosyProfilePresenter;
import org.smartregister.chw.tbleprosy.domain.MemberObject;

import timber.log.Timber;

public class CoreTbLeprosyMemberProfilePresenter extends BaseTbLeprosyProfilePresenter implements CoreTbLeprosyProfileContract.Presenter {

    private MemberObject tbMemberObject;

    public CoreTbLeprosyMemberProfilePresenter(CoreTbLeprosyProfileContract.View view, CoreTbLeprosyProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
        this.interactor = interactor;
        this.tbMemberObject = memberObject;
    }

    @Override
    public CoreTbLeprosyProfileContract.View getView() {
        if (view != null) {
            return (CoreTbLeprosyProfileContract.View) view.get();
        }
        return null;
    }

    public void startTbLeprosyReferral() {
        try {
            getView().startFormActivity((new FormUtils()).getFormJsonFromRepositoryOrAssets(((CoreTbLeprosyProfileActivity) getView()), CoreConstants.JSON_FORM.getTbLeprosyReferralForm()), tbMemberObject);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

}
