package org.smartregister.chw.core.presenter;

import android.app.Activity;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.contract.MotherMentorProfileContract;
import org.smartregister.chw.mothermentor.presenter.BaseMotherMentorProfilePresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.contract.FamilyProfileContract;
import org.smartregister.family.domain.FamilyEventClient;

import timber.log.Timber;

public class CoreMotherMentorProfilePresenter extends BaseMotherMentorProfilePresenter implements FamilyProfileContract.InteractorCallBack {

    public CoreMotherMentorProfilePresenter(MotherMentorProfileContract.View view, MotherMentorProfileContract.Interactor interactor, MemberObject memberObject) {
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
    public void onRegistrationSaved(boolean isEditMode, boolean isSaved, FamilyEventClient familyEventClient) {
        ((Activity) getView()).finish();
    }
}

