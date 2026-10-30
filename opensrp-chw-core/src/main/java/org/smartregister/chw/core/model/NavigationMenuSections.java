package org.smartregister.chw.core.model;

import java.util.ArrayList;
import java.util.Collections;
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
    private final Set<String> followUpItems;
    private boolean expanded;

    public NavigationMenuSections(List<NavigationOption> options, List<String> primaryItems) {
        this(options, primaryItems, Collections.emptyList());
    }

    public NavigationMenuSections(List<NavigationOption> options, List<String> primaryItems,
                                  List<String> followUpItems) {
        this.options = options;
        this.primaryItems = new LinkedHashSet<>(primaryItems);
        this.followUpItems = new LinkedHashSet<>(followUpItems);
        this.followUpItems.removeAll(this.primaryItems);
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
            if (!primaryItems.contains(option.getMenuTitle()) && !followUpItems.contains(option.getMenuTitle())) {
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

        boolean hasFollowUpHeader = false;
        for (String followUpItem : followUpItems) {
            for (NavigationOption option : options) {
                if (followUpItem.equals(option.getMenuTitle())) {
                    if (!hasFollowUpHeader) {
                        rows.add(new Row(null, false, true));
                        hasFollowUpHeader = true;
                    }
                    rows.add(new Row(option, false));
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
        if (primaryItems.isEmpty() || primaryItems.contains(menuTitle) || followUpItems.contains(menuTitle)) {
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
        private final boolean followUpHeader;

        private Row(NavigationOption option, boolean child) {
            this(option, child, false);
        }

        private Row(NavigationOption option, boolean child, boolean followUpHeader) {
            this.option = option;
            this.child = child;
            this.followUpHeader = followUpHeader;
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

        public boolean isFollowUpHeader() {
            return followUpHeader;
        }
    }
}
