package org.smartregister.chw.core.presenter;

import android.app.Activity;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.asrh.contract.AsrhProfileContract;
import org.smartregister.chw.asrh.domain.MemberObject;
import org.smartregister.chw.asrh.presenter.BaseAsrhProfilePresenter;
import org.smartregister.chw.cecap.contract.CecapProfileContract;
import org.smartregister.chw.cecap.presenter.BaseCecapProfilePresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;

import timber.log.Timber;

public class CoreAsrhProfilePresenter extends BaseAsrhProfilePresenter implements FamilyProfileContract.InteractorCallBack {

    public CoreAsrhProfilePresenter(AsrhProfileContract.View view, AsrhProfileContract.Interactor interactor, MemberObject memberObject) {
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
        ((Activity)getView()).finish();
    }
}
