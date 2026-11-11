package org.smartregister.chw.core.custom_views;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.contract.NavigationContract;
import org.smartregister.chw.core.interactor.NavigationInteractor;
import org.smartregister.chw.core.model.NavigationModel;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.Collections;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class NavigationMenuInitializationTest {
    private NavigationInteractor previousInteractor;
    private NavigationModel previousModel;
    private CoreChwApplication previousApplication;
    private NavigationMenu.Flavour previousMenuFlavor;
    private NavigationModel.Flavor previousModelFlavor;
    private NavigationInteractor interactor;
    private HashMap<String, String> flavorTables;
    private TestNavigationMenu menu;

    @Before
    public void setUp() {
        previousInteractor = ReflectionHelpers.getStaticField(NavigationInteractor.class, "instance");
        previousModel = ReflectionHelpers.getStaticField(NavigationModel.class, "instance");
        previousApplication = NavigationMenu.application;
        previousMenuFlavor = NavigationMenu.menuFlavor;
        previousModelFlavor = NavigationMenu.modelFlavor;
        interactor = mock(NavigationInteractor.class);
        ReflectionHelpers.setStaticField(NavigationInteractor.class, "instance", interactor);
        ReflectionHelpers.setStaticField(NavigationModel.class, "instance", null);
        NavigationMenu.application = null;
        NavigationMenu.menuFlavor = mock(NavigationMenu.Flavour.class);
        NavigationMenu.modelFlavor = mock(NavigationModel.Flavor.class);
        flavorTables = new HashMap<>();
        when(NavigationMenu.menuFlavor.getTableMapValues()).thenReturn(flavorTables);
        doAnswer(
                        invocation -> {
                            NavigationContract.InteractorCallback<Integer> callback =
                                    invocation.getArgument(1);
                            callback.onResult(7);
                            return null;
                        })
                .when(interactor)
                .getRegisterCount(any(), any());
        menu = new TestNavigationMenu();
    }

    @After
    public void tearDown() {
        NavigationMenu.application = previousApplication;
        NavigationMenu.menuFlavor = previousMenuFlavor;
        NavigationMenu.modelFlavor = previousModelFlavor;
        ReflectionHelpers.setStaticField(NavigationInteractor.class, "instance", previousInteractor);
        ReflectionHelpers.setStaticField(NavigationModel.class, "instance", previousModel);
    }

    @Test
    public void firstSoberHouseCountUsesTheFlavorMappingBeforeViewsRequestCounts() {
        String table = "ec_harm_reduction_sober_house_enrollment";
        NavigationOption option = option(CoreConstants.DrawerMenu.HARM_REDUCTION_SOBER_HOUSE);
        when(NavigationMenu.modelFlavor.getNavigationItems())
                .thenReturn(Collections.singletonList(option));
        flavorTables.put(CoreConstants.DrawerMenu.HARM_REDUCTION_SOBER_HOUSE, table);

        menu.init(null, null, null);

        verify(interactor).getRegisterCount(org.mockito.ArgumentMatchers.eq(table), any());
        assertEquals(7, option.getRegisterCount());
        assertEquals(1, menu.countRefreshes);
    }

    @Test
    public void flavorOverrideAppliesToTheFirstRequestForAnExistingDefaultRoute() {
        NavigationOption option = option(CoreConstants.DrawerMenu.ALL_CLIENTS);
        when(NavigationMenu.modelFlavor.getNavigationItems())
                .thenReturn(Collections.singletonList(option));
        flavorTables.put(CoreConstants.DrawerMenu.ALL_CLIENTS, "ec_selected_clients");

        menu.init(null, null, null);

        verify(interactor)
                .getRegisterCount(org.mockito.ArgumentMatchers.eq("ec_selected_clients"), any());
        assertEquals(7, option.getRegisterCount());
    }

    @Test
    public void emptyFlavorOverridesPreserveTheDefaultClientTableMapping() {
        NavigationOption option = option(CoreConstants.DrawerMenu.ALL_CLIENTS);
        when(NavigationMenu.modelFlavor.getNavigationItems())
                .thenReturn(Collections.singletonList(option));

        menu.init(null, null, null);

        verify(interactor)
                .getRegisterCount(
                        org.mockito.ArgumentMatchers.eq(CoreConstants.TABLE_NAME.FAMILY_MEMBER),
                        any());
        assertEquals(7, option.getRegisterCount());
    }

    private NavigationOption option(String title) {
        return new NavigationOption(0, 0, 0, title, 0);
    }

    private static class TestNavigationMenu extends NavigationMenu {
        private int countRefreshes;

        @Override
        protected void setParentView(Activity activity, View parentView) {
            // Keep this regression focused on real presenter/count initialization.
        }

        @Override
        public void prepareViews(Activity activity) {
            mPresenter.refreshNavigationCount(activity);
        }

        @Override
        public void refreshCount() {
            countRefreshes++;
        }
    }
}
