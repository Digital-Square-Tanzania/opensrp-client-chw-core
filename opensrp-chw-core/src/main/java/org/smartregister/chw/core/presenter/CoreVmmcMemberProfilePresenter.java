package org.smartregister.chw.core.presenter;

import org.smartregister.chw.core.contract.CoreVmmcProfileContract;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.vmmc.domain.MemberObject;
import org.smartregister.chw.vmmc.presenter.BaseVmmcProfilePresenter;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.FormUtils;

import timber.log.Timber;

public class CoreVmmcMemberProfilePresenter extends BaseVmmcProfilePresenter implements CoreVmmcProfileContract.Presenter {
    private FormUtils formUtils;
    private CoreVmmcProfileContract.Interactor interactor;

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


    private FormUtils getFormUtils() {
        if (formUtils == null) {
            try {
                formUtils = FormUtils.getInstance(Utils.context().applicationContext());
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return formUtils;
    }
}
