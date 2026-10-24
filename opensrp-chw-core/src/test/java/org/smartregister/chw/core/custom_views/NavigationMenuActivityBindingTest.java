package org.smartregister.chw.core.custom_views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.adapter.NavigationAdapter;
import org.smartregister.chw.core.contract.NavigationContract;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class NavigationMenuActivityBindingTest {

    private final List<ActivityController<? extends RecordingActivity>> controllers =
            new ArrayList<>();
    private NavigationMenu.Flavour previousMenuFlavor;
    private Map<String, Class> previousRegisteredActivities;
    private NavigationMenu menu;

    @Before
    public void setUp() {
        previousMenuFlavor = NavigationMenu.menuFlavor;
        previousRegisteredActivities =
                ReflectionHelpers.getStaticField(NavigationMenu.class, "registeredActivities");
        NavigationMenu.menuFlavor = mock(NavigationMenu.Flavour.class);
        when(NavigationMenu.menuFlavor.getPrimaryNavigationItems())
                .thenReturn(Collections.singletonList(CoreConstants.DrawerMenu.ALL_CLIENTS));

        Map<String, Class> activities = new HashMap<>();
        activities.put(
                CoreConstants.REGISTERED_ACTIVITIES.MOTHER_MENTOR_REGISTER_ACTIVITY,
                MotherMentorActivity.class);
        activities.put(
                CoreConstants.REGISTERED_ACTIVITIES.ANC_REGISTER_ACTIVITY, AncActivity.class);
        ReflectionHelpers.setStaticField(NavigationMenu.class, "registeredActivities", activities);

        menu = new NavigationMenu();
        menu.mPresenter = mock(NavigationContract.Presenter.class);
        when(menu.mPresenter.getOptions())
                .thenReturn(
                        Arrays.asList(
                                option(CoreConstants.DrawerMenu.ALL_CLIENTS),
                                option(CoreConstants.DrawerMenu.MOTHER_MENTOR),
                                option(CoreConstants.DrawerMenu.ANC)));
    }

    @After
    public void tearDown() {
        NavigationMenu.menuFlavor = previousMenuFlavor;
        ReflectionHelpers.setStaticField(
                NavigationMenu.class, "registeredActivities", previousRegisteredActivities);
        for (ActivityController<? extends RecordingActivity> controller : controllers) {
            controller.pause().stop().destroy();
        }
    }

    @Test
    public void firstBindingLaunchesMotherMentorFromTheCurrentHost() {
        AllClientsActivity host = create(AllClientsActivity.class).get();
        bind(host);

        click(CoreConstants.DrawerMenu.MOTHER_MENTOR, host);

        assertLaunch(host, MotherMentorActivity.class);
    }

    @Test
    public void reboundMenuDoesNotLaunchFromTheDestroyedPreviousHost() {
        ActivityController<AllClientsActivity> previous = create(AllClientsActivity.class);
        bind(previous.get());
        destroy(previous);
        AncActivity current = create(AncActivity.class).get();
        bind(current);

        click(CoreConstants.DrawerMenu.MOTHER_MENTOR, current);

        assertLaunch(current, MotherMentorActivity.class);
        assertTrue(previous.get().launches.isEmpty());
        assertEquals(0, previous.get().finishes);
    }

    @Test
    public void recreatedHostUsesItsNewActivityInstance() {
        ActivityController<AllClientsActivity> previous = create(AllClientsActivity.class);
        bind(previous.get());
        destroy(previous);
        AllClientsActivity current = create(AllClientsActivity.class).get();
        bind(current);

        click(CoreConstants.DrawerMenu.MOTHER_MENTOR, current);

        assertLaunch(current, MotherMentorActivity.class);
        assertTrue(previous.get().launches.isEmpty());
    }

    @Test
    public void returningToMotherMentorDoesNotReuseItsDestroyedActivityAsTheCaller() {
        ActivityController<MotherMentorActivity> previous = create(MotherMentorActivity.class);
        bind(previous.get());
        destroy(previous);
        AllClientsActivity current = create(AllClientsActivity.class).get();
        bind(current);

        click(CoreConstants.DrawerMenu.MOTHER_MENTOR, current);

        assertLaunch(current, MotherMentorActivity.class);
        assertTrue(previous.get().launches.isEmpty());
        assertEquals(0, previous.get().finishes);
    }

    @Test
    @Config(qualifiers = "sw")
    public void swahiliAncRowKeepsItsInvariantRouteAndUsesTheCurrentHost() {
        ActivityController<MotherMentorActivity> previous = create(MotherMentorActivity.class);
        bind(previous.get());
        destroy(previous);
        AllClientsActivity current = create(AllClientsActivity.class).get();
        bind(current);

        NavigationAdapter.MyViewHolder row = row(CoreConstants.DrawerMenu.ANC, current);
        assertEquals("Wajawazito", row.tvName.getText().toString());
        assertEquals(CoreConstants.DrawerMenu.ANC, row.getView().getTag());
        assertTrue(row.getView().performClick());

        assertLaunch(current, AncActivity.class);
        assertTrue(previous.get().launches.isEmpty());
    }

    @Test
    public void rebindingPreservesSelectionAndManuallyCollapsedModules() {
        AllClientsActivity previous = create(AllClientsActivity.class).get();
        bind(previous);
        NavigationAdapter adapter = menu.getNavigationAdapter();
        adapter.setSelectedView(CoreConstants.DrawerMenu.MOTHER_MENTOR);
        assertEquals(4, adapter.getItemCount());
        assertTrue(bindRow(1, previous).getView().performClick());
        assertEquals(2, adapter.getItemCount());

        AncActivity current = create(AncActivity.class).get();
        bind(current);

        assertSame(adapter, menu.getNavigationAdapter());
        assertEquals(CoreConstants.DrawerMenu.MOTHER_MENTOR, adapter.getSelectedView());
        assertEquals(2, adapter.getItemCount());
        assertFalse(current.isFinishing());
    }

    private <T extends RecordingActivity> ActivityController<T> create(Class<T> type) {
        ActivityController<T> controller = Robolectric.buildActivity(type).setup();
        controller.get().setTheme(R.style.Theme_AppCompat);
        controllers.add(controller);
        return controller;
    }

    private void destroy(ActivityController<? extends RecordingActivity> controller) {
        controller.pause().stop().destroy();
        controllers.remove(controller);
    }

    private void bind(RecordingActivity host) {
        RecyclerView recyclerView = new RecyclerView(host);
        host.setContentView(recyclerView);
        ReflectionHelpers.setField(menu, "recyclerView", recyclerView);
        ReflectionHelpers.callInstanceMethod(
                menu,
                "registerNavigation",
                ReflectionHelpers.ClassParameter.from(Activity.class, host));
    }

    private void click(String route, RecordingActivity host) {
        assertTrue(row(route, host).getView().performClick());
    }

    private NavigationAdapter.MyViewHolder row(String route, RecordingActivity host) {
        NavigationAdapter adapter = menu.getNavigationAdapter();
        adapter.setSelectedView(route);
        for (int position = 0; position < adapter.getItemCount(); position++) {
            NavigationAdapter.MyViewHolder holder = bindRow(position, host);
            if (route.equals(holder.getView().getTag())) {
                return holder;
            }
        }
        throw new AssertionError("Missing navigation route: " + route);
    }

    private NavigationAdapter.MyViewHolder bindRow(int position, RecordingActivity host) {
        NavigationAdapter adapter = menu.getNavigationAdapter();
        NavigationAdapter.MyViewHolder holder =
                adapter.onCreateViewHolder(new FrameLayout(host), adapter.getItemViewType(position));
        adapter.onBindViewHolder(holder, position);
        return holder;
    }

    private NavigationOption option(String route) {
        return new NavigationOption(
                R.mipmap.sidemenu_anc,
                R.mipmap.sidemenu_anc_active,
                R.string.menu_anc,
                route,
                0);
    }

    private void assertLaunch(RecordingActivity host, Class<? extends Activity> target) {
        assertEquals(1, host.launches.size());
        assertEquals(target.getName(), host.launches.get(0).getComponent().getClassName());
        assertTrue((host.launches.get(0).getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertEquals(1, host.finishes);
    }

    public static class RecordingActivity extends Activity {
        final List<Intent> launches = new ArrayList<>();
        int finishes;

        @Override
        public void startActivity(Intent intent) {
            launches.add(intent);
        }

        @Override
        public void finish() {
            finishes++;
        }
    }

    public static class AllClientsActivity extends RecordingActivity {}

    public static class MotherMentorActivity extends RecordingActivity {}

    public static class AncActivity extends RecordingActivity {}
}
