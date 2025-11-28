package org.smartregister.chw.core.presenter;

import android.app.Activity;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.hts.contract.HtsProfileContract;
import org.smartregister.chw.hts.domain.MemberObject;
import org.smartregister.chw.hts.presenter.BaseHtsProfilePresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;

import timber.log.Timber;

/**
 * Created by ilakozejumanne@gmail.com on 17/12/2024.
 */
public class CoreHtsProfilePresenter extends BaseHtsProfilePresenter implements FamilyProfileContract.InteractorCallBack {
    public CoreHtsProfilePresenter(HtsProfileContract.View view, HtsProfileContract.Interactor interactor, MemberObject memberObject) {
        super(view, interactor, memberObject);
    }

    @Override
    public void startFormForEdit(CommonPersonObjectClient commonPersonObjectClient) {
        Timber.d("unimplemented");
    }

    @Override
    public void refreshProfileTopSection(CommonPersonObjectClient commonPersonObjectClient) {
        Timber.d("unimplemented");
    }

    @Override
    public void onUniqueIdFetched(Triple<String, String, String> triple, String s) {
        Timber.d("unimplemented");
    }

    @Override
    public void onNoUniqueId() {
        Timber.d("unimplemented");
    }

    @Override
    public void onRegistrationSaved(boolean b, boolean b1, FamilyEventClient familyEventClient) {
        ((Activity) getView()).finish();
    }
}
