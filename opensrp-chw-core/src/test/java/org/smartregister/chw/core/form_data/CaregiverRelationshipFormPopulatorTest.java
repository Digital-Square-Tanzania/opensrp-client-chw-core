package org.smartregister.chw.core.form_data;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.util.JsonFormUtils;

public class CaregiverRelationshipFormPopulatorTest {

    private static final String[] HELPER_KEYS = {
            "caregiver_relationship_base",
            "caregiver_relationship_adult",
            "caregiver_relationship_in_law",
            "caregiver_relationship_in_law_adult",
            "caregiver_relationship_wife",
            "caregiver_relationship_wife_adult",
            "caregiver_relationship_husband",
            "caregiver_relationship_husband_adult"
    };

    @Test
    public void populateSetsAdultHelperForSingleAdult() throws JSONException {
        JSONArray fields = createFields("Yes", "20", "Male", "Single", "Mother");

        CaregiverRelationshipFormPopulator.populate(fields, null);

        assertOnlyHelperHasValue(fields, "caregiver_relationship_adult", "Mother");
    }

    @Test
    public void populateSetsWifeAdultHelperForMarriedAdultMale() throws JSONException {
        JSONArray fields = createFields("Yes", "20", "Male", "Married", "Wife");

        CaregiverRelationshipFormPopulator.populate(fields, null);

        assertOnlyHelperHasValue(fields, "caregiver_relationship_wife_adult", "Wife");
    }

    @Test
    public void populateSetsHusbandAdultHelperForMarriedAdultFemale() throws JSONException {
        JSONArray fields = createFields("Yes", "20", "Female", "Married", "Husband");

        CaregiverRelationshipFormPopulator.populate(fields, null);

        assertOnlyHelperHasValue(fields, "caregiver_relationship_husband_adult", "Husband");
    }

    @Test
    public void populateDoesNotSetHelperWhenClientHasNoCaregiver() throws JSONException {
        JSONArray fields = createFields("No", "20", "Female", "Married", "Husband");

        CaregiverRelationshipFormPopulator.populate(fields, null);

        assertOnlyHelperHasValue(fields, null, null);
    }

    private JSONArray createFields(String hasCaregiver, String age, String sex,
                                   String maritalStatus, String relationship) throws JSONException {
        JSONArray fields = new JSONArray();
        fields.put(createField("has_primary_caregiver", hasCaregiver));
        fields.put(createField("age", age));
        fields.put(createField("sex", sex));
        fields.put(createField("marital_status", maritalStatus));

        JSONArray relationshipKeys = new JSONArray()
                .put("Mother")
                .put("Wife")
                .put("Husband");
        for (String helperKey : HELPER_KEYS) {
            fields.put(createField(helperKey, null).put("keys", relationshipKeys));
        }

        fields.put(createField("caregiver_relationship", relationship));
        return fields;
    }

    private JSONObject createField(String key, String value) throws JSONException {
        JSONObject field = new JSONObject().put(JsonFormConstants.KEY, key);
        if (value != null) {
            field.put(JsonFormConstants.VALUE, value);
        }
        return field;
    }

    private void assertOnlyHelperHasValue(JSONArray fields, String populatedKey, String expectedValue) throws JSONException {
        for (String helperKey : HELPER_KEYS) {
            JSONObject helper = JsonFormUtils.getFieldJSONObject(fields, helperKey);
            if (helperKey.equals(populatedKey)) {
                Assert.assertEquals(expectedValue, helper.optString(JsonFormConstants.VALUE));
            } else {
                Assert.assertFalse(helper.has(JsonFormConstants.VALUE));
            }
        }
    }
}
