package org.smartregister.chw.core.utils;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.opd.activity.BaseOpdFormActivity;
import org.smartregister.opd.utils.OpdConstants;

import static org.smartregister.chw.core.utils.CoreReferralUtils.getCommonRepository;

public class UpdateDetailsUtil {
    @NotNull
    public static CommonPersonObjectClient getFamilyRegistrationDetails(String familyBaseEntityId) {
        //Update common person client object with all details from family register table
        final CommonPersonObject personObject = getCommonRepository(Utils.metadata().familyRegister.tableName)
                .findByBaseEntityId(familyBaseEntityId);
        CommonPersonObjectClient commonPersonObjectClient = new CommonPersonObjectClient(personObject.getCaseId(),
                personObject.getDetails(), "");
        commonPersonObjectClient.setColumnmaps(personObject.getColumnmaps());
        commonPersonObjectClient.setDetails(personObject.getDetails());
        return commonPersonObjectClient;
    }

    public static void startUpdateClientDetailsActivity(JSONObject jsonForm, Activity activity) {
        Intent intent = new Intent(activity, BaseOpdFormActivity.class);
        intent.putExtra(OpdConstants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        Form form = new Form();
        form.setName(activity.getString(org.smartregister.chw.core.R.string.update_client_registration));
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setPreviousLabel(activity.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        activity.startActivityForResult(intent, org.smartregister.family.util.JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    /**
     * Use this for ANC and PNC Member profile to get FamilyBaseEntityID
     *
     * @param client commonPersonObject client
     * @return familyBaseEntityId
     */
    public static String getFamilyBaseEntityId(CommonPersonObjectClient client) {
        return org.smartregister.util.Utils.getValue(client.getColumnmaps(), org.smartregister.family.util.DBConstants.KEY.RELATIONAL_ID, false);
    }

    /**
     * Use this to determine if client is independent, family members won't have the location info option menu
     *
     * @param baseEntityId baseEntityId
     * @return true if client is independent and false if its a family member
     */
    public static boolean isIndependentClient(String baseEntityId) {
        final CommonPersonObject personObject = getCommonRepository(Utils.metadata().familyMemberRegister.tableName)
                .findByBaseEntityId(baseEntityId);
        String entityType = org.smartregister.util.Utils.getValue(personObject.getColumnmaps(), DBConstants.KEY.ENTITY_TYPE, false);
        return entityType.equalsIgnoreCase("ec_independent_client");
    }
}
