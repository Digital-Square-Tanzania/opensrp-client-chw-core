package org.smartregister.chw.core.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NavigationMenuSectionsTest {
    private static final List<String> PRIMARY = Arrays.asList("clients", "households", "icchw");

    @Test
    public void emptyPrimaryConfigurationPreservesFlatDrawer() {
        List<NavigationOption> options = options("anc", "households", "clients", "icchw");
        NavigationMenuSections sections = new NavigationMenuSections(options, Collections.emptyList());

        assertEquals(Arrays.asList("anc", "households", "clients", "icchw"), keys(sections));
        sections.revealSelection("anc");
        assertFalse(sections.isExpanded());
        for (NavigationMenuSections.Row row : sections.getRows()) {
            assertFalse(row.isChild());
        }
    }

    @Test
    public void primaryItemsAreOrderedAndRemainingModulesStartCollapsed() {
        List<NavigationOption> options = options("anc", "icchw", "households", "clients", "hiv");
        NavigationMenuSections sections = new NavigationMenuSections(options, PRIMARY);

        assertEquals(Arrays.asList("clients", "households", "icchw", null), keys(sections));
        assertEquals(Arrays.asList("anc", "icchw", "households", "clients", "hiv"), optionKeys(options));
        assertSame(options.get(3), sections.getRows().get(0).getOption());
        assertFalse(sections.isExpanded());
    }

    @Test
    public void unavailablePrimaryRoutesAreNeverAdded() {
        NavigationMenuSections sections = new NavigationMenuSections(options("anc", "clients"), PRIMARY);

        assertEquals(Arrays.asList("clients", null), keys(sections));
        sections.setExpanded(true);
        assertEquals(Arrays.asList("clients", null, "anc"), keys(sections));
        assertTrue(sections.getRows().get(2).isChild());
    }

    @Test
    public void expandedSectionRetainsHeaderAndOriginalModuleOrder() {
        List<NavigationOption> options = options("anc", "icchw", "hiv", "clients", "households", "tb");
        NavigationMenuSections sections = new NavigationMenuSections(options, PRIMARY);
        sections.setExpanded(true);

        assertEquals(Arrays.asList("clients", "households", "icchw", null, "anc", "hiv", "tb"), keys(sections));
        assertTrue(sections.getRows().get(3).isHeader());
        assertSame(options.get(0), sections.getRows().get(4).getOption());
        sections.setExpanded(false);
        assertEquals(Arrays.asList("clients", "households", "icchw", null), keys(sections));
    }

    @Test
    public void headerIsOmittedWhenNoOtherModulesAreAvailable() {
        NavigationMenuSections sections = new NavigationMenuSections(options("icchw", "clients"), PRIMARY);
        sections.setExpanded(true);

        assertEquals(Arrays.asList("clients", "icchw"), keys(sections));
        assertTrue(new NavigationMenuSections(Collections.emptyList(), PRIMARY).getRows().isEmpty());
    }

    @Test
    public void readsUseCurrentSourceAndKeepManualCollapseDuringCountRefresh() {
        List<NavigationOption> options = options("clients", "anc");
        NavigationMenuSections sections = new NavigationMenuSections(options, PRIMARY);
        sections.revealSelection("anc");
        assertTrue(sections.isExpanded());
        options.get(1).setRegisterCount(42);
        assertEquals(42, sections.getRows().get(2).getOption().getRegisterCount());

        sections.setExpanded(false);
        options.get(1).setRegisterCount(43);
        assertEquals(Arrays.asList("clients", null), keys(sections));
        assertFalse(sections.isExpanded());

        options.clear();
        options.addAll(options("icchw", "tb"));
        assertEquals(Arrays.asList("icchw", null), keys(sections));
        sections.setExpanded(true);
        assertEquals(Arrays.asList("icchw", null, "tb"), keys(sections));
    }

    @Test
    public void selectionExpandsOnlyAnAvailableOtherModule() {
        NavigationMenuSections sections = new NavigationMenuSections(options("clients", "anc"), PRIMARY);
        sections.revealSelection("clients");
        assertFalse(sections.isExpanded());
        sections.revealSelection("hidden-role-module");
        assertFalse(sections.isExpanded());
        sections.revealSelection("anc");
        assertTrue(sections.isExpanded());
    }

    @Test
    public void duplicatePrimaryConfigurationDoesNotDuplicateRoutes() {
        NavigationMenuSections sections = new NavigationMenuSections(options("anc", "clients"),
                Arrays.asList("clients", "clients"));

        assertEquals(Arrays.asList("clients", null), keys(sections));
    }

    private List<NavigationOption> options(String... keys) {
        List<NavigationOption> options = new ArrayList<>();
        for (String key : keys) {
            options.add(new NavigationOption(1, 2, 3, key, 0));
        }
        return options;
    }

    private List<String> keys(NavigationMenuSections sections) {
        List<String> keys = new ArrayList<>();
        for (NavigationMenuSections.Row row : sections.getRows()) {
            keys.add(row.isHeader() ? null : row.getOption().getMenuTitle());
        }
        return keys;
    }

    private List<String> optionKeys(List<NavigationOption> options) {
        List<String> keys = new ArrayList<>();
        for (NavigationOption option : options) {
            keys.add(option.getMenuTitle());
        }
        return keys;
    }
}
