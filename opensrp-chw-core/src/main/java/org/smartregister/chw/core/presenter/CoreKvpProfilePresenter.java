package org.smartregister.chw.core.presenter;

import android.app.Activity;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.kvp.contract.KvpProfileContract;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.presenter.BaseKvpProfilePresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;

import timber.log.Timber;

public class CoreKvpProfilePresenter extends BaseKvpProfilePresenter implements FamilyProfileContract.InteractorCallBack {
    public CoreKvpProfilePresenter(KvpProfileContract.View view, KvpProfileContract.Interactor interactor, MemberObject memberObject) {
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
