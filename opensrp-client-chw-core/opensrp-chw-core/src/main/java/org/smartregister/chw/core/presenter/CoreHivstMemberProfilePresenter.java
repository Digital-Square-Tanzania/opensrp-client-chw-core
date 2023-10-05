package org.smartregister.chw.core.presenter;

import org.smartregister.chw.core.contract.CoreHivstProfileContract;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hivst.domain.MemberObject;
import org.smartregister.chw.hivst.presenter.BaseHivstProfilePresenter;

import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.FormUtils;

import timber.log.Timber;

public class CoreHivstMemberProfilePresenter extends BaseHivstProfilePresenter implements CoreHivstProfileContract.Presenter {

    private FormUtils formUtils;
    private CoreHivstProfileContract.Interactor interactor;

    public CoreHivstMemberProfilePresenter(CoreHivstProfileContract.View view, CoreHivstProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
        this.interactor = interactor;
    }

    @Override
    public CoreHivstProfileContract.View getView() {
        if (view != null) {
            return (CoreHivstProfileContract.View) view.get();
        }
        return null;
    }

    @Override
    public void startHivstFollowupForm() {
        //TODO change the form to the correct followup form
        try {
            getView().startFormActivity(getFormUtils().getFormJson(CoreConstants.JSON_FORM.getPmtctFollowupForm()));
        } catch (Exception ex) {
            Timber.e(ex);
        }
    }

    @Override
    public void createHivstFollowupEvent(AllSharedPreferences allSharedPreferences, String jsonString, String entityID) throws Exception {
        interactor.createHivstFollowupEvent(allSharedPreferences, jsonString, entityID);
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
