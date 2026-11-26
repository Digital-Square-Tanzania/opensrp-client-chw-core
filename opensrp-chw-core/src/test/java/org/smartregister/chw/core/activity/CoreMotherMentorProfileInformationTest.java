package org.smartregister.chw.core.activity;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.core.BaseUnitTest;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.form_data.DataLoader;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.shadows.LocationHelperShadowHelper;
import org.smartregister.chw.core.shadows.LocationPickerViewShadowHelper;
import org.smartregister.chw.core.shadows.UtilsShadowUtil;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.mothermentor.domain.MemberObject;
import org.smartregister.chw.mothermentor.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Client;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.view.activity.BaseProfileActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Reproduces the information-menu failures using persisted client/family fixtures. */
@Config(shadows = {CoreMotherMentorProfileInformationTest.ClientLookup.class,
        CoreMotherMentorProfileInformationTest.FamilyLookup.class,
        CoreMotherMentorProfileInformationTest.FormPopulation.class,
        CoreMotherMentorProfileInformationTest.Binder.class,
        UtilsShadowUtil.class, LocationHelperShadowHelper.class,
        LocationPickerViewShadowHelper.class})
public class CoreMotherMentorProfileInformationTest extends BaseUnitTest {
    private static final String CLIENT_ID = "test-client";
    private static final String FAMILY_ID = "persisted-family";
    private static final String UNIQUE_ID = "123-456";
    private static final String MEMBER_UPDATE = "Update Family Member Registration";
    private static final String FAMILY_UPDATE = "Update Family Registration";
    private ProfileActivity activity;
    private MemberObject member;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(ProfileActivity.class).get();
        member = new MemberObject();
        member.setBaseEntityId(CLIENT_ID);
        member.setFamilyBaseEntityId("stale-family");
        member.setFamilyName("Test family");
        member.setLastName("RecordedSurname");
        member.setPrimaryCareGiver(CLIENT_ID);
        activity.member(member);
        ClientLookup.client = person(CLIENT_ID, UNIQUE_ID, FAMILY_ID);
        ClientLookup.missing = false;
        FamilyLookup.independent = true;
        FamilyLookup.missing = false;
        FamilyLookup.lookups.clear();
        FamilyLookup.forms.clear();
        Binder.returnNull = false;
        Binder.formName = null;
        Binder.boundClient = null;
        FormPopulation.eventType = null;
        FamilyMetadata metadata = new FamilyMetadata(FamilyWizardFormActivity.class,
                FamilyWizardFormActivity.class, BaseProfileActivity.class,
                CoreConstants.IDENTIFIER.UNIQUE_IDENTIFIER_KEY, false);
        metadata.updateFamilyRegister("family_register", "ec_family", "Family Registration",
                FAMILY_UPDATE, "config", "family_head", "primary_caregiver");
        metadata.updateFamilyMemberRegister("family_member_register", "ec_family_member",
                "Family Member Registration", MEMBER_UPDATE, "config", "relational_id");
        UtilsShadowUtil.setMetadata(metadata);
    }

    @Test
    public void locationUsesPersistedRelationshipInsteadOfStaleProfileFamily() {
        assertTrue(select(R.id.action_location_info));
        assertEquals(Collections.singletonList(FAMILY_ID), FamilyLookup.lookups);
        assertEquals(1, launches());
        assertEquals(FAMILY_ID, FamilyLookup.forms.get(0).optString("entity_id"));
        assertEquals(FAMILY_UPDATE, FamilyLookup.forms.get(0).optString("encounter_type"));
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliLocationUsesPersistedRelationshipWhenProfileFamilyIsEmpty() {
        member.setFamilyBaseEntityId("");
        locationUsesPersistedRelationshipInsteadOfStaleProfileFamily();
    }

    @Test
    public void independentInformationUsesOwnRecordedDetailsWithoutHouseholdLookup() throws Exception {
        member.setFamilyBaseEntityId("");
        assertTrue(select(R.id.action_registration));
        assertIndependentInformation("Yes");
        assertTrue(FamilyLookup.lookups.isEmpty());
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliIndependentInformationPreservesNonCaregiverAndRecordedDetails() throws Exception {
        member.setFamilyBaseEntityId(null);
        member.setPrimaryCareGiver("another-client");
        assertTrue(select(R.id.action_registration));
        assertIndependentInformation("No");
        assertTrue(FamilyLookup.lookups.isEmpty());
    }

    @Test
    public void independentInformationNeverLaunchesASecondNullFormOrUsesFamilyIdentifier() throws Exception {
        member.setFamilyBaseEntityId(FAMILY_ID);
        assertTrue(select(R.id.action_registration));
        assertIndependentInformation("Yes");
        assertTrue(FamilyLookup.lookups.isEmpty());
    }

    @Test
    public void independentInformationWithNullFamilyNameKeepsRecordedSurname() throws Exception {
        member.setFamilyBaseEntityId(FAMILY_ID);
        member.setFamilyName(null);
        assertTrue(select(R.id.action_registration));
        assertIndependentInformation("Yes");
    }

    @Test
    public void householdMemberStillUsesPopulatedMemberEditForm() {
        FamilyLookup.independent = false;
        assertTrue(select(R.id.action_registration));
        assertEquals(1, launches());
        assertEquals(MEMBER_UPDATE, activity.forms.get(0).optString("encounter_type"));
        assertEquals(CLIENT_ID, activity.forms.get(0).optString("entity_id"));
        assertEquals(MEMBER_UPDATE, FormPopulation.eventType);
        assertTrue(FamilyLookup.forms.isEmpty());
    }

    @Test
    public void missingLocationFamilyDoesNotEscapeTheMenuHandler() {
        FamilyLookup.missing = true;
        assertTrue(select(R.id.action_location_info));
        assertEquals(0, launches());
    }

    @Test
    public void missingLocationClientDoesNotLaunchInformationFromStaleProfileData() {
        ClientLookup.missing = true;
        member.setFamilyBaseEntityId(FAMILY_ID);
        assertTrue(select(R.id.action_location_info));
        assertEquals(0, launches());
    }

    @Test
    public void missingRegistrationClientDoesNotEscapeTheMenuHandler() {
        ClientLookup.missing = true;
        assertTrue(select(R.id.action_registration));
        assertEquals(0, launches());
    }

    @Test
    public void unavailablePrepopulatedFormDoesNotLaunchANullOrBlankForm() {
        member.setFamilyBaseEntityId(FAMILY_ID);
        Binder.returnNull = true;
        assertTrue(select(R.id.action_registration));
        assertEquals(0, launches());
    }

    private void assertIndependentInformation(String caregiver) throws Exception {
        assertEquals(1, launches());
        assertTrue(activity.forms.isEmpty());
        assertEquals(CLIENT_ID, Binder.boundClient);
        assertEquals(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm(), Binder.formName);
        JSONObject form = FamilyLookup.forms.get(0);
        assertEquals(CLIENT_ID, form.getString("entity_id"));
        assertEquals(MEMBER_UPDATE, form.getString("encounter_type"));
        assertEquals(UNIQUE_ID, form.getString("current_opensrp_id"));
        assertEquals(activity.getString(R.string.registration_info), form.getJSONObject("step1").getString("title"));
        assertEquals("RecordedFirst", value(form, "first_name"));
        assertEquals("RecordedSurname", value(form, "surname"));
        assertEquals("02-03-1995", value(form, "dob"));
        assertEquals("123456", value(form, "unique_id"));
        assertEquals(caregiver, value(form, "is_primary_caregiver"));
        assertEquals("test_location_id", form.getJSONObject("metadata").getString("encounter_location"));
    }

    private int launches() { return activity.forms.size() + FamilyLookup.forms.size(); }

    private boolean select(int id) {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(id);
        return activity.onOptionsItemSelected(item);
    }

    private String value(JSONObject form, String key) throws Exception {
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) return field.optString("value");
        }
        throw new AssertionError("Missing form field: " + key);
    }

    private static CommonPersonObjectClient person(String id, String uniqueId, String familyId) {
        Map<String, String> values = new HashMap<>();
        values.put(DBConstants.KEY.BASE_ENTITY_ID, id);
        values.put(DBConstants.KEY.UNIQUE_ID, uniqueId);
        values.put(DBConstants.KEY.RELATIONAL_ID, familyId);
        CommonPersonObjectClient person = new CommonPersonObjectClient(id, values, "");
        person.setColumnmaps(values);
        return person;
    }

    public static class ProfileActivity extends CoreMotherMentorProfileActivity {
        final List<JSONObject> forms = new ArrayList<>();
        void member(MemberObject object) { memberObject = object; }
        @Override public void startFormActivity(JSONObject form) { forms.add(form); }
        @Override public void startHivstRegistration() { }
        @Override public void openObservationResults() { }
        @Override public void continueContactVisit() { }
        @Override public void continueService() { }
        @Override public void observationResults() { }
        @Override public void openClientObservationResults() { }
        @Override public void openRecordClientVisit() { }
        @Override public void openRecordTbContactVisit() { }
        @Override public void openTbContactFollowUpVisit() { }
        @Override public void startServiceForm() { }
    }

    @Implements(org.smartregister.chw.core.utils.Utils.class)
    public static class ClientLookup {
        static CommonPersonObjectClient client;
        static boolean missing;
        @Implementation public static CommonPersonObjectClient clientForEdit(String id) {
            if (missing) throw new NullPointerException("Missing saved client fixture");
            return client;
        }
        @Implementation public static CommonPersonObjectClient getCommonPersonObjectClient(String id) {
            return clientForEdit(id);
        }
    }

    @Implements(UpdateDetailsUtil.class)
    public static class FamilyLookup {
        static boolean independent;
        static boolean missing;
        static final List<String> lookups = new ArrayList<>();
        static final List<JSONObject> forms = new ArrayList<>();
        @Implementation public static boolean isIndependentClient(String id) { return independent; }
        @Implementation public static CommonPersonObjectClient getFamilyRegistrationDetails(String id) {
            lookups.add(id);
            if (missing || !FAMILY_ID.equals(id)) throw new NullPointerException("Family record not found for fixture ID");
            return person(FAMILY_ID, UNIQUE_ID + "-family", "");
        }
        @Implementation public static void startUpdateClientDetailsActivity(JSONObject form, Activity activity) {
            forms.add(form);
        }
    }

    @Implements(CoreJsonFormUtils.class)
    public static class FormPopulation {
        static String eventType;
        @Implementation public static JSONObject getAutoPopulatedJsonEditFormString(String name,
                Context context, CommonPersonObjectClient client, String event) throws Exception {
            eventType = event;
            return new JSONObject().put("entity_id", client.getCaseId()).put("encounter_type", event);
        }
        @Implementation public static JSONObject getAutoPopulatedJsonEditMemberFormString(String title,
                String name, Context context, CommonPersonObjectClient client, String event,
                String familyName, boolean caregiver) throws Exception {
            return getAutoPopulatedJsonEditFormString(name, context, client, event);
        }
    }

    /** Keeps the real configured data loader and its field/metadata binding, replacing only persistence. */
    @Implements(NativeFormsDataBinder.class)
    public static class Binder {
        static boolean returnNull;
        static String formName;
        static String boundClient;
        Context context;
        String clientId;
        DataLoader loader;
        @Implementation public void __constructor__(Context context, String id) {
            this.context = context;
            clientId = id;
        }
        @Implementation public void setDataLoader(DataLoader dataLoader) { loader = dataLoader; }
        @Implementation public JSONObject getPrePopulatedForm(String name) throws Exception {
            formName = name;
            boundClient = clientId;
            if (returnNull) return null;
            Client client = mock(Client.class);
            when(client.getBaseEntityId()).thenReturn(clientId);
            when(client.getFirstName()).thenReturn("RecordedFirst");
            when(client.getLastName()).thenReturn("RecordedSurname");
            when(client.getIdentifier("opensrp_id")).thenReturn(UNIQUE_ID);
            when(client.getBirthdate()).thenReturn(new DateTime(1995, 3, 2, 0, 0));
            ReflectionHelpers.setField(loader, "client", client);
            ReflectionHelpers.setField(loader, "tableMap", new HashMap<>());
            ReflectionHelpers.setField(loader, "obsMap", new HashMap<>());
            JSONArray fields = new JSONArray()
                    .put(field("first_name", "person", "first_name"))
                    .put(field("surname", "person", "last_name"))
                    .put(field("dob", "person", "birthdate"))
                    .put(field("unique_id", "person_identifier", "opensrp_id"))
                    .put(field("is_primary_caregiver", "concept", "is_primary_caregiver"));
            JSONObject form = new JSONObject().put("count", "1")
                    .put("encounter_type", "Original registration event")
                    .put("metadata", new JSONObject().put("encounter_location", ""))
                    .put("step1", new JSONObject().put("title", "Original title").put("fields", fields));
            loader.loadForm(context, form, clientId);
            loader.bindNativeFormsMetaData(form, context, clientId);
            return form;
        }
        private JSONObject field(String key, String entity, String entityId) throws Exception {
            return new JSONObject().put("key", key).put("type", "edit_text")
                    .put("openmrs_entity", entity).put("openmrs_entity_id", entityId);
        }
    }
}
