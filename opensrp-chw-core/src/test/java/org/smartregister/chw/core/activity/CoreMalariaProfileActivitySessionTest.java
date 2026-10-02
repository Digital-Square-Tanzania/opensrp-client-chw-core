package org.smartregister.chw.core.activity;

import android.content.Intent;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.core.BaseUnitTest;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.shadows.ContextShadow;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.malaria.contract.MalariaProfileContract;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.malaria.domain.MemberObject;
import org.smartregister.chw.malaria.util.Constants;
import org.smartregister.service.ZiggyService;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.controller.ANMController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Exercises the real secured lifecycle and parent profile creation with a synthetic DAO result. */
@Config(application = CoreMalariaProfileActivitySessionTest.SessionApplication.class,
        shadows = {CoreMalariaProfileActivitySessionTest.SessionContext.class,
                CoreMalariaProfileActivitySessionTest.MemberDao.class})
public class CoreMalariaProfileActivitySessionTest extends BaseUnitTest {
    private ActivityController<ProfileActivity> controller;

    @Before
    public void setUp() {
        SessionContext.loggedOut = false;
        MemberDao.lookups = 0;
        MemberDao.lastBaseEntityId = null;
        MemberDao.member = new MemberObject();
        MemberDao.member.setFamilyName("Test family");
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.pause().stop().destroy();
        }
    }

    @Test
    public void expiredSessionSkipsProfileInitialization() {
        SessionContext.loggedOut = true;
        create(profileIntent(R.string.return_to_family_name));

        assertLoggedOutWithoutProfile();
    }

    @Test
    public void expiredSessionWithMissingPayloadCanCreateAndResume() {
        SessionContext.loggedOut = true;
        create(new Intent());
        controller.start().resume();
        Robolectric.flushForegroundThreadScheduler();

        assertLoggedOutWithoutProfile();
        assertTrue(application().logoutCount >= 2);
    }

    @Test
    public void authenticatedCreationSetsFamilyTitleAndNotifications() {
        create(profileIntent(R.string.return_to_family_name));
        ProfileActivity activity = controller.get();

        assertEquals(1, MemberDao.lookups);
        assertEquals("test-member", MemberDao.lastBaseEntityId);
        assertEquals(activity.getString(R.string.return_to_family_name, "Test family"), title());
        assertNotNull(activity.notificationAndReferralLayout);
        assertTrue(activity.notificationAndReferralRecyclerView.getLayoutManager()
                instanceof LinearLayoutManager);
        assertEquals(1, activity.notificationInitializations);
        verify(activity.testPresenter()).fillProfileData(MemberDao.member);
        assertEquals(0, application().logoutCount);
    }

    @Test
    public void authenticatedCreationPreservesNonFamilyTitle() {
        create(profileIntent(R.string.return_to_all_client));

        assertEquals(controller.get().getString(R.string.return_to_all_client), title());
    }

    @Test
    public void resumingAfterLogoutDoesNotReinitializeProfile() {
        create(profileIntent(R.string.return_to_family_name));
        controller.start().resume().pause();
        int initializations = controller.get().notificationInitializations;
        SessionContext.loggedOut = true;
        controller.resume();
        Robolectric.flushForegroundThreadScheduler();

        assertTrue(application().logoutCount > 0);
        assertEquals(1, MemberDao.lookups);
        assertEquals("test-member", MemberDao.lastBaseEntityId);
        assertEquals(initializations, controller.get().notificationInitializations);
    }

    private void create(Intent intent) {
        controller = Robolectric.buildActivity(ProfileActivity.class, intent);
        controller.get().setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
        controller.create();
    }

    private Intent profileIntent(int title) {
        return new Intent().putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, "test-member")
                .putExtra(CoreConstants.INTENT_KEY.TOOLBAR_TITLE, title);
    }

    private String title() {
        return ((TextView) controller.get().findViewById(R.id.toolbar_title)).getText().toString();
    }

    private SessionApplication application() {
        return (SessionApplication) RuntimeEnvironment.application;
    }

    private void assertLoggedOutWithoutProfile() {
        assertTrue(application().logoutCount > 0);
        assertEquals(0, MemberDao.lookups);
        assertNull(controller.get().testMember());
        assertNull(controller.get().notificationAndReferralRecyclerView);
        assertEquals(0, controller.get().notificationInitializations);
    }

    public static class SessionApplication extends DrishtiApplication {
        private int logoutCount;

        @Override
        public void onCreate() {
            mInstance = this;
            CoreLibrary.init(Context.getInstance().updateApplicationContext(this));
            setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
        }

        @Override
        public void logoutCurrentUser() {
            logoutCount++;
        }
    }

    @Implements(Context.class)
    public static class SessionContext extends ContextShadow {
        private static boolean loggedOut;

        @Implementation
        public ZiggyService ziggyService() {
            return null;
        }

        @Implementation
        public ANMController anmController() {
            return null;
        }

        // Robolectric 4.3 matches the existing Context API by its exact method name.
        @SuppressWarnings("PMD.MethodNamingConventions")
        @Implementation
        public boolean IsUserLoggedOut() {
            return loggedOut;
        }
    }

    @Implements(MalariaDao.class)
    public static class MemberDao {
        private static MemberObject member;
        private static int lookups;
        private static String lastBaseEntityId;

        @Implementation
        public static MemberObject getMember(String baseEntityId) {
            lookups++;
            lastBaseEntityId = baseEntityId;
            return member;
        }
    }

    public static class ProfileActivity extends CoreMalariaProfileActivity {
        private int notificationInitializations;

        private MemberObject testMember() {
            return memberObject;
        }

        private MalariaProfileContract.Presenter testPresenter() {
            return profilePresenter;
        }

        @Override
        protected void initializePresenter() {
            profilePresenter = mock(MalariaProfileContract.Presenter.class);
        }

        @Override
        public void initializeFloatingMenu() {
            // Keep the real parent view setup without unrelated menu dependencies.
        }

        @Override
        protected void initializeNotificationReferralRecyclerView() {
            super.initializeNotificationReferralRecyclerView();
            notificationInitializations++;
        }

        @Override
        protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
            return CoreFamilyProfileActivity.class;
        }

        @Override
        protected void removeMember() {
            // Not used by these lifecycle tests.
        }

        @Override
        public CoreFamilyOtherMemberActivityPresenter presenter() {
            return mock(CoreFamilyOtherMemberActivityPresenter.class);
        }

        @Override
        public android.content.Context getContext() {
            return this;
        }

        @Override
        public void refreshList() {
            // Not used by these lifecycle tests.
        }

        @Override
        public void updateHasPhone(boolean hasPhone) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void setFamilyServiceStatus(String status) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void verifyHasPhone() {
            // Not used by these lifecycle tests.
        }

        @Override
        public void notifyHasPhone(boolean hasPhone) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void onEventSaveComplete(boolean success) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void setProfileImage(String id, String type) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void setProfileDetailThree(String detail) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void toggleFamilyHead(boolean show) {
            // Not used by these lifecycle tests.
        }

        @Override
        public void togglePrimaryCaregiver(boolean show) {
            // Not used by these lifecycle tests.
        }

    }
}
