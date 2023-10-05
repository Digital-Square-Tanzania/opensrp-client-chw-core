package org.smartregister.chw.core.activity;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.getAutoPopulatedJsonEditFormString;
import static org.smartregister.chw.hiv.util.Constants.ActivityPayload.HIV_MEMBER_OBJECT;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.dataloader.CoreFamilyMemberDataLoader;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.kvp.activity.BaseKvpProfileActivity;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.AlertStatus;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.util.Date;

import timber.log.Timber;

/**
 * Created by Billy on 20/09/2022.
 */
public abstract class CoreKvpProfileActivity extends BaseKvpProfileActivity {
    //TODO: implement startFormActivity, to load followup forms

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        rlLastVisit.setVisibility(View.GONE);
    }

    @Override
    public void refreshFamilyStatus(AlertStatus status) {
        super.refreshFamilyStatus(status);
        rlFamilyServicesDue.setVisibility(View.GONE);
    }

    @Override
    public void refreshUpComingServicesStatus(String service, AlertStatus status, Date date) {
        rlUpcomingServices.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.kvp_profile_menu, menu);
        menu.findItem(R.id.action_location_info).setVisible(UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId()));
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_registration) {
            if (UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId())) {
                startFormForEdit(R.string.registration_info,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            } else {
                startFormForEdit(R.string.edit_member_form_title,
                        CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }
            return true;
        } else if (itemId == R.id.action_location_info) {
            JSONObject preFilledForm = getAutoPopulatedJsonEditFormString(
                    CoreConstants.JSON_FORM.getFamilyDetailsRegister(), this,
                    UpdateDetailsUtil.getFamilyRegistrationDetails(memberObject.getFamilyBaseEntityId()), Utils.metadata().familyRegister.updateEventType);
            if (preFilledForm != null)
                UpdateDetailsUtil.startUpdateClientDetailsActivity(preFilledForm, this);
            return true;
        } else if (itemId == R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startFormForEdit(Integer title_resource, String formName) {

        JSONObject form = null;
        CommonPersonObjectClient client = org.smartregister.chw.core.utils.Utils.clientForEdit(memberObject.getBaseEntityId());

        if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {
            form = CoreJsonFormUtils.getAutoPopulatedJsonEditMemberFormString(
                    (title_resource != null) ? getResources().getString(title_resource) : null,
                    CoreConstants.JSON_FORM.getFamilyMemberRegister(),
                    this, client,
                    Utils.metadata().familyMemberRegister.updateEventType, memberObject.getLastName(), false);
        } else if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {
            form = CoreJsonFormUtils.getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), this, formName, CoreConstants.EventType.UPDATE_ANC_REGISTRATION, getResources().getString(title_resource));
        } else if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;
            CommonPersonObjectClient commonPersonObjectClient = UpdateDetailsUtil.getFamilyRegistrationDetails(memberObject.getFamilyBaseEntityId());
            String uniqueID = commonPersonObjectClient.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);
            boolean isPrimaryCareGiver = commonPersonObjectClient.getCaseId().equalsIgnoreCase(memberObject.getFamilyBaseEntityId());

            NativeFormsDataBinder binder = new NativeFormsDataBinder(getContext(), memberObject.getBaseEntityId());
            binder.setDataLoader(new CoreFamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString,
                    org.smartregister.chw.core.utils.Utils.metadata().familyMemberRegister.updateEventType, uniqueID));
            JSONObject jsonObject = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());

            try {
                if (jsonObject != null) {
                    UpdateDetailsUtil.startUpdateClientDetailsActivity(jsonObject, this);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        try {
            assert form != null;
            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public Context getContext() {
        return this;
    }

    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = org.smartregister.chw.core.utils.Utils.formActivityIntent(this, jsonForm.toString());
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    public abstract void startHivstRegistration();

}
