package org.smartregister.chw.core.activity.impl;

import android.content.Context;

import org.mockito.Mockito;
import org.smartregister.chw.core.activity.CoreFamilyOtherMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.custom_views.CoreFamilyMemberFloatingMenu;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.fragment.BaseFamilyOtherMemberProfileFragment;
import org.smartregister.view.contract.BaseProfileContract;

public class CoreFamilyOtherMemberProfileActivityImpl extends CoreFamilyOtherMemberProfileActivity {
    @Override
    public void startAncRegister() {
        // do nothing
    }

    @Override
    protected void startPncRegister() {
        // do nothing
    }

    @Override
    public void startFpRegister() {
        // do nothing
    }

    @Override
    public void startMalariaRegister() {
        // do nothing
    }

    @Override
    public void startFpEcpScreening() {
        // do nothing
    }

    @Override
    protected void startVmmcRegister() {
        // do nothing
    }

    @Override
    protected void startTbLeprosyScreening() {
        // do nothing
    }

    @Override
    protected void startIntegratedCommunityCaseManagementEnrollment() {
        // do nothing
    }

    @Override
    protected void startHivRegister() {
        // do nothing
    }

    @Override
    protected void startTbRegister() {
        // do nothing
    }

    @Override
    protected void startHarmReductionAssessment() {
        // do nothing
    }

    @Override
    protected void startHarmReductionSoberHouseEnrollment() {
        // do nothing
    }

    @Override
    public void startMalariaFollowUpVisit() {
        // do nothing
    }

    @Override
    public void startHfMalariaFollowupForm() {
        // do nothing
    }

    @Override
    protected void startPmtctRegisration() {
        // do nothing
    }

    @Override
    protected void startLDRegistration() {
        // do nothing
    }

    @Override
    protected void startHivstRegistration() {
        // do nothing
    }

    @Override
    protected void startKvpPrEPRegistration() {
        // do nothing
    }

    @Override
    protected void startKvpRegistration() {
        // do nothing
    }

    @Override
    protected void startPrEPRegistration() {
        // do nothing
    }

    @Override
    protected void startAgywScreening() {
        // do nothing
    }

    @Override
    protected void startDiabetesRiskAssessment() {
        // do nothing
    }

    @Override
    protected void startSbcRegistration() {
        // do nothing
    }

    @Override
    protected void startGbvRegistration() {
        // do nothing
    }

    @Override
    protected void startCancerPreventiveServicesRegistration() {
        // do nothing
    }

    @Override
    protected void startAsrhRegistration() {
        // do nothing
    }

    @Override
    protected void startHtsScreening() {
        // do nothing
    }

    @Override
    protected void startHpsEnrollment() {
        // do nothing
    }

    @Override
    protected void startAypFacilityScreening() {
        // do nothing
    }

    @Override
    protected void startAypInSchoolEnrollment() {
        // do nothing
    }

    @Override
    protected void startAypParentalEnrollment() {
        // do nothing
    }

    @Override
    protected void startAypOutSchoolEnrollment() {
        // do nothing
    }

    @Override
    protected void startMotherMentorEnrollIit() {
        // do nothing
    }

    @Override
    protected void startMotherMentorEnrollPartner() {
        // do nothing
    }

    @Override
    protected void startMotherMentorEnrollChildEid() {
        // do nothing
    }

    @Override
    public void setIndependentClient(boolean isIndependent) {
        // do nothing
    }

    @Override
    public void removeIndividualProfile() {
        // do nothing
    }

    @Override
    public void startEditMemberJsonForm(Integer title_resource, CommonPersonObjectClient client) {
        // do nothing
    }

    @Override
    protected BaseProfileContract.Presenter getFamilyOtherMemberActivityPresenter(String familyBaseEntityId, String baseEntityId, String familyHead, String primaryCaregiver, String villageTown, String familyName) {
        return Mockito.mock(CoreFamilyOtherMemberActivityPresenter.class);
    }

    @Override
    protected CoreFamilyMemberFloatingMenu getFamilyMemberFloatingMenu() {
        return Mockito.mock(CoreFamilyMemberFloatingMenu.class);
    }

    @Override
    protected Context getFamilyOtherMemberProfileActivity() {
        return null;
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivity() {
        return null;
    }

    @Override
    protected BaseFamilyOtherMemberProfileFragment getFamilyOtherMemberProfileFragment() {
        return null;
    }
}
