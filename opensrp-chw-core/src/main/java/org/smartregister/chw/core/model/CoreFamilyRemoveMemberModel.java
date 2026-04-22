package org.smartregister.chw.core.model;


import static org.smartregister.chw.core.utils.Utils.getDuration;
import static org.smartregister.family.util.JsonFormUtils.processFamilyHeadRegistrationForm;
import static org.smartregister.family.util.JsonFormUtils.processFamilyUpdateForm;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensrp.api.constants.Gender;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.FamilyRemoveMemberContract;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.util.FormUtils;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public abstract class CoreFamilyRemoveMemberModel extends CoreFamilyProfileMemberModel implements FamilyRemoveMemberContract.Model {
    private FormUtils formUtils;

    public static String getGenderTranslated(Context context, String gender) {
        if (gender.equalsIgnoreCase(Gender.MALE.toString())) {
            return context.getResources().getString(R.string.male);
        } else if (gender.equalsIgnoreCase(Gender.FEMALE.toString())) {
            return context.getResources().getString(R.string.female);
        }
        return "";
    }

    @Override
    public JSONObject prepareJsonForm(CommonPersonObjectClient client, String formType) {
        try {
            FormUtils formUtils = FormUtils.getInstance(Utils.context().applicationContext());
            JSONObject form = formUtils.getFormJson(formType);

            form.put(JsonFormUtils.ENTITY_ID, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false));
            // inject data into the form

            JSONObject stepOne = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
            JSONArray jsonArray = stepOne.getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.DOB)) {

                    String dobString = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
                    if (StringUtils.isNotBlank(dobString)) {
                        Date dob = Utils.dobStringToDate(dobString);
                        if (dob != null) {
                            jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, JsonFormUtils.dd_MM_yyyy.format(dob));
                            JSONObject min_date = CoreJsonFormUtils.getFieldJSONObject(jsonArray, "date_moved");
                            JSONObject date_died = CoreJsonFormUtils.getFieldJSONObject(jsonArray, "date_died");

                            // dobString = Utils.getDuration(dobString);
                            //dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : "";
                            int days = CoreJsonFormUtils.getDayFromDate(dobString);
                            min_date.put("min_date", "today-" + days + "d");
                            date_died.put("min_date", "today-" + days + "d");
                        }
                    }
                } else if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.DETAILS)) {

                    String dob = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
                    String dobString = getDuration(dob);
                    dobString = dobString.contains("y") ? dobString.substring(0, dobString.indexOf("y")) : dobString;

                    String details = String.format("%s %s %s, %s %s",
                            Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true),
                            Utils.getValue(client.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true),
                            Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LAST_NAME, true),
                            dobString,
                            getGenderTranslated(Utils.context().applicationContext(), Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, true))
                    );

                    jsonObject.put("text", details);

                } else if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.FIRST_NAME)) {
                    jsonObject.put("value", Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true));
                } else if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.MIDDLE_NAME)) {
                    jsonObject.put("value", Utils.getValue(client.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true));
                } else if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.LAST_NAME)) {
                    jsonObject.put("value", Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LAST_NAME, true));
                } else if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.SEX)) {
                    jsonObject.put("value", Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GENDER, true).toLowerCase(Locale.getDefault()));
                }
            }

            return form;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public String getForm(CommonPersonObjectClient client) {
        Date dob = Utils.dobStringToDate(Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false));
        return ((dob != null && getDiffYears(dob, new Date()) >= 5) ? CoreConstants.JSON_FORM.getFamilyDetailsRemoveMember() : CoreConstants.JSON_FORM.getFamilyDetailsRemoveChild());
    }

    @Override
    public JSONObject prepareFamilyRemovalForm(String familyID, String familyName, String details) {
        try {
            FormUtils formUtils = FormUtils.getInstance(Utils.context().applicationContext());
            JSONObject form = formUtils.getFormJson(CoreConstants.JSON_FORM.getFamilyDetailsRemoveFamily());
            form.put(JsonFormUtils.ENTITY_ID, familyID);

            JSONObject stepOne = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
            JSONArray jsonArray = stepOne.getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.DETAILS)) {
                    jsonObject.put("text", details);
                }
                if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(CoreConstants.JsonAssets.FAM_NAME)) {
                    jsonObject.put("text", familyName);
                }
            }

            return form;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    public int getDiffYears(Date first, Date last) {
        Calendar a = getCalendar(first);
        Calendar b = getCalendar(last);
        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) ||
                (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
            diff--;
        }
        return diff;
    }

    public Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(date);
        return cal;
    }

    @Override
    public List<FamilyEventClient> processFamilyMemberRemoval(String jsonString) {
        List<FamilyEventClient> familyEventClientList = new ArrayList<>();
        FamilyEventClient familyEventClient = processFamilyUpdateForm(Utils.context().allSharedPreferences(), jsonString);
        if (familyEventClient == null) {
            return familyEventClientList;
        }

        FamilyEventClient headEventClient = processFamilyHeadRegistrationForm(Utils.context().allSharedPreferences(), jsonString, familyEventClient.getClient().getBaseEntityId());
        if (headEventClient == null) {
            return familyEventClientList;
        }

        if (headEventClient.getClient() != null && familyEventClient.getClient() != null) {
            String headUniqueId = headEventClient.getClient().getIdentifier(Utils.metadata().uniqueIdentifierKey);
            if (StringUtils.isNotBlank(headUniqueId)) {
                String familyUniqueId = headUniqueId + Constants.IDENTIFIER.FAMILY_SUFFIX;
                familyEventClient.getClient().addIdentifier(Utils.metadata().uniqueIdentifierKey, familyUniqueId);
            }
        }

        // Update the family head and primary caregiver
        Client familyClient = familyEventClient.getClient();
        familyClient.addRelationship(Utils.metadata().familyRegister.familyHeadRelationKey, headEventClient.getClient().getBaseEntityId());
        familyClient.addRelationship(Utils.metadata().familyRegister.familyCareGiverRelationKey, headEventClient.getClient().getBaseEntityId());

        familyEventClientList.add(familyEventClient);
        familyEventClientList.add(headEventClient);
        return familyEventClientList;
    }

    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String baseEntityId, String currentLocationId) throws Exception {
        JSONObject form = this.getFormUtils().getFormJson(formName);
        return form == null ? null : org.smartregister.family.util.JsonFormUtils.getFormAsJson(form, formName, entityId, currentLocationId);
    }

    @Override
    public String getLocationId(String locationName) {
        return LocationHelper.getInstance().getOpenMrsLocationId(locationName);
    }

    protected FormUtils getFormUtils() {
        if (this.formUtils == null) {
            try {
                this.formUtils = FormUtils.getInstance(Utils.context().applicationContext());
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        return this.formUtils;
    }
}

