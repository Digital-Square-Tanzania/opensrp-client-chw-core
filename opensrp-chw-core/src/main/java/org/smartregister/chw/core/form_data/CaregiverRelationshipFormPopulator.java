package org.smartregister.chw.core.form_data;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Client;
import org.smartregister.util.JsonFormUtils;

public final class CaregiverRelationshipFormPopulator {

    private static final String CAREGIVER_RELATIONSHIP = "caregiver_relationship";
    private static final String CAREGIVER_RELATIONSHIP_BASE = "caregiver_relationship_base";
    private static final String CAREGIVER_RELATIONSHIP_ADULT = "caregiver_relationship_adult";
    private static final String CAREGIVER_RELATIONSHIP_IN_LAW = "caregiver_relationship_in_law";
    private static final String CAREGIVER_RELATIONSHIP_IN_LAW_ADULT = "caregiver_relationship_in_law_adult";
    private static final String CAREGIVER_RELATIONSHIP_WIFE = "caregiver_relationship_wife";
    private static final String CAREGIVER_RELATIONSHIP_WIFE_ADULT = "caregiver_relationship_wife_adult";
    private static final String CAREGIVER_RELATIONSHIP_HUSBAND = "caregiver_relationship_husband";
    private static final String CAREGIVER_RELATIONSHIP_HUSBAND_ADULT = "caregiver_relationship_husband_adult";
    private static final String HAS_PRIMARY_CAREGIVER = "has_primary_caregiver";
    private static final String MARITAL_STATUS = "marital_status";
    private static final String SEX = "sex";
    private static final String AGE = "age";
    private static final String AGE_CALCULATED = "age_calculated";
    private static final String[] CAREGIVER_RELATIONSHIP_HELPER_KEYS = {
            CAREGIVER_RELATIONSHIP_BASE,
            CAREGIVER_RELATIONSHIP_ADULT,
            CAREGIVER_RELATIONSHIP_IN_LAW,
            CAREGIVER_RELATIONSHIP_IN_LAW_ADULT,
            CAREGIVER_RELATIONSHIP_WIFE,
            CAREGIVER_RELATIONSHIP_WIFE_ADULT,
            CAREGIVER_RELATIONSHIP_HUSBAND,
            CAREGIVER_RELATIONSHIP_HUSBAND_ADULT
    };

    private CaregiverRelationshipFormPopulator() {
    }

    public static boolean hasSavedRelationship(JSONArray fields) throws JSONException {
        JSONObject caregiverRelationship = JsonFormUtils.getFieldJSONObject(fields, CAREGIVER_RELATIONSHIP);
        return caregiverRelationship != null
                && StringUtils.isNotBlank(caregiverRelationship.optString(JsonFormConstants.VALUE));
    }

    public static void populate(JSONArray fields, Client client) throws JSONException {
        JSONObject caregiverRelationship = JsonFormUtils.getFieldJSONObject(fields, CAREGIVER_RELATIONSHIP);
        if (caregiverRelationship == null) {
            return;
        }

        String selectedRelationship = caregiverRelationship.optString(JsonFormConstants.VALUE);
        if (StringUtils.isBlank(selectedRelationship)) {
            return;
        }

        String targetKey = getTargetHelperKey(fields, client);
        if (StringUtils.isBlank(targetKey)) {
            return;
        }

        clearHelperValues(fields);

        JSONObject targetField = JsonFormUtils.getFieldJSONObject(fields, targetKey);
        if (targetField != null && spinnerContainsKey(targetField, selectedRelationship)) {
            targetField.put(JsonFormConstants.VALUE, selectedRelationship);
        }
    }

    private static String getTargetHelperKey(JSONArray fields, Client client) throws JSONException {
        if (!"Yes".equalsIgnoreCase(getFieldValue(fields, HAS_PRIMARY_CAREGIVER))) {
            return null;
        }

        Integer age = getAge(fields, client);
        boolean adult = age != null && age >= 18;
        boolean atLeastFifteen = age != null && age >= 15;
        String maritalStatus = getFieldValue(fields, MARITAL_STATUS);
        boolean nonSingleMaritalStatus = StringUtils.isNotBlank(maritalStatus) && !"Single".equalsIgnoreCase(maritalStatus);
        boolean spouseMaritalStatus = nonSingleMaritalStatus
                && !"Divorced".equalsIgnoreCase(maritalStatus)
                && !"Widowed".equalsIgnoreCase(maritalStatus);
        String sex = getFieldValue(fields, SEX);

        if (!nonSingleMaritalStatus) {
            return adult ? CAREGIVER_RELATIONSHIP_ADULT : CAREGIVER_RELATIONSHIP_BASE;
        }

        if ("Male".equalsIgnoreCase(sex) && atLeastFifteen && spouseMaritalStatus) {
            return adult ? CAREGIVER_RELATIONSHIP_WIFE_ADULT : CAREGIVER_RELATIONSHIP_WIFE;
        }

        if ("Female".equalsIgnoreCase(sex) && atLeastFifteen && spouseMaritalStatus) {
            return adult ? CAREGIVER_RELATIONSHIP_HUSBAND_ADULT : CAREGIVER_RELATIONSHIP_HUSBAND;
        }

        return adult ? CAREGIVER_RELATIONSHIP_IN_LAW_ADULT : CAREGIVER_RELATIONSHIP_IN_LAW;
    }

    private static void clearHelperValues(JSONArray fields) throws JSONException {
        for (String helperKey : CAREGIVER_RELATIONSHIP_HELPER_KEYS) {
            JSONObject helperField = JsonFormUtils.getFieldJSONObject(fields, helperKey);
            if (helperField != null) {
                helperField.remove(JsonFormConstants.VALUE);
            }
        }
    }

    private static String getFieldValue(JSONArray fields, String key) throws JSONException {
        JSONObject field = JsonFormUtils.getFieldJSONObject(fields, key);
        return field == null ? "" : field.optString(JsonFormConstants.VALUE);
    }

    private static Integer getAge(JSONArray fields, Client client) throws JSONException {
        Integer age = parseAge(getFieldValue(fields, AGE_CALCULATED));
        if (age != null) {
            return age;
        }

        age = parseAge(getFieldValue(fields, AGE));
        if (age != null) {
            return age;
        }

        if (client != null && client.getBirthdate() != null) {
            return Years.yearsBetween(client.getBirthdate(), new DateTime()).getYears();
        }

        return null;
    }

    private static Integer parseAge(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            return (int) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean spinnerContainsKey(JSONObject field, String selectedRelationship) {
        JSONArray keys = field.optJSONArray("keys");
        if (keys == null || keys.length() == 0) {
            return true;
        }

        for (int i = 0; i < keys.length(); i++) {
            if (selectedRelationship.equals(keys.optString(i))) {
                return true;
            }
        }
        return false;
    }
}
