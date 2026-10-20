package org.smartregister.chw.core.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Projects the current role's options into drawer rows without changing the source
 * list used to refresh register counts.
 */
public class NavigationMenuSections {
    private final List<NavigationOption> options;
    private final Set<String> primaryItems;
    private boolean expanded;

    public NavigationMenuSections(List<NavigationOption> options, List<String> primaryItems) {
        this.options = options;
        this.primaryItems = new LinkedHashSet<>(primaryItems);
    }

    public List<Row> getRows() {
        List<Row> rows = new ArrayList<>();
        if (primaryItems.isEmpty()) {
            for (NavigationOption option : options) {
                rows.add(new Row(option, false));
            }
            return rows;
        }

        for (String primaryItem : primaryItems) {
            for (NavigationOption option : options) {
                if (primaryItem.equals(option.getMenuTitle())) {
                    rows.add(new Row(option, false));
                }
            }
        }

        List<NavigationOption> otherItems = new ArrayList<>();
        for (NavigationOption option : options) {
            if (!primaryItems.contains(option.getMenuTitle())) {
                otherItems.add(option);
            }
        }
        if (!otherItems.isEmpty()) {
            rows.add(new Row(null, false));
            if (expanded) {
                for (NavigationOption option : otherItems) {
                    rows.add(new Row(option, true));
                }
            }
        }
        return rows;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void revealSelection(String menuTitle) {
        if (primaryItems.isEmpty() || primaryItems.contains(menuTitle)) {
            return;
        }
        for (NavigationOption option : options) {
            if (option.getMenuTitle().equals(menuTitle)) {
                expanded = true;
                return;
            }
        }
    }

    public static class Row {
        private final NavigationOption option;
        private final boolean child;

        private Row(NavigationOption option, boolean child) {
            this.option = option;
            this.child = child;
        }

        public NavigationOption getOption() {
            return option;
        }

        public boolean isHeader() {
            return option == null;
        }

        public boolean isChild() {
            return child;
        }
    }
}
