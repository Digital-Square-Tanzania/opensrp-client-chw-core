package org.smartregister.chw.core.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.smartregister.chw.core.R;
import org.smartregister.chw.core.listener.NavigationListener;
import org.smartregister.chw.core.model.NavigationMenuSections;
import org.smartregister.chw.core.model.NavigationOption;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.MyViewHolder> {

    private static final int VIEW_TYPE_OPTION = 0;
    private static final int VIEW_TYPE_OTHER_MODULES = 1;
    private static final int VIEW_TYPE_FOLLOW_UP_HEADER = 2;
    private final NavigationMenuSections sections;
    private String selectedView = CoreConstants.DrawerMenu.ALL_FAMILIES;
    private View.OnClickListener onClickListener;
    private Context context;
    private Map<String, Class> registeredActivities;

    public NavigationAdapter(List<NavigationOption> navigationOptions, Activity context, Map<String, Class> registeredActivities) {
        this(navigationOptions, context, registeredActivities, Collections.emptyList());
    }

    public NavigationAdapter(List<NavigationOption> navigationOptions, Activity context,
                             Map<String, Class> registeredActivities, List<String> primaryNavigationItems) {
        this(navigationOptions, context, registeredActivities, primaryNavigationItems, Collections.emptyList());
    }

    public NavigationAdapter(List<NavigationOption> navigationOptions, Activity context,
                             Map<String, Class> registeredActivities, List<String> primaryNavigationItems,
                             List<String> followUpNavigationItems) {
        this.sections = new NavigationMenuSections(navigationOptions, primaryNavigationItems, followUpNavigationItems);
        this.registeredActivities = registeredActivities;
        bindToActivity(context);
    }

    public void bindToActivity(@NonNull Activity activity) {
        this.context = activity;
        this.onClickListener = new NavigationListener(activity, this);
    }

    public String getSelectedView() {
        if (selectedView == null || selectedView.equals(""))
            setSelectedView(CoreConstants.DrawerMenu.ALL_FAMILIES);

        return selectedView;
    }

    public void setSelectedView(String selectedView) {
        if (this.selectedView == null || !this.selectedView.equals(selectedView)) {
            sections.revealSelection(selectedView);
        }
        this.selectedView = selectedView;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_TYPE_FOLLOW_UP_HEADER ? R.layout.navigation_follow_up_header
                : viewType == VIEW_TYPE_OTHER_MODULES ? R.layout.navigation_other_modules : R.layout.navigation_item;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        NavigationMenuSections.Row row = sections.getRows().get(position);
        if (row.isFollowUpHeader()) {
            bindFollowUpHeader(holder);
            return;
        }
        if (row.isHeader()) {
            bindOtherModules(holder);
            return;
        }
        NavigationOption model = row.getOption();
        View view = holder.getView();
        int indent = row.isChild()
                ? context.getResources().getDimensionPixelSize(R.dimen.navigation_module_indent) : 0;
        view.setPaddingRelative(holder.paddingStart + indent, view.getPaddingTop(),
                holder.paddingEnd, view.getPaddingBottom());
        holder.tvName.setText(context.getResources().getText(model.getTitleID()));
        holder.tvCount.setText(String.format(Locale.getDefault(), "%d", model.getRegisterCount()));
        holder.ivIcon.setImageResource(model.getResourceID());

        holder.getView().setTag(model.getMenuTitle());
        holder.getView().setOnClickListener(onClickListener);


        if (selectedView != null && selectedView.equals(model.getMenuTitle())) {
            holder.tvCount.setTextColor(context.getResources().getColor(R.color.navigation_item_selected));
            holder.tvName.setTextColor(context.getResources().getColor(R.color.navigation_item_selected));
            holder.ivIcon.setImageResource(model.getResourceActiveID());
        } else {
            holder.tvCount.setTextColor(context.getResources().getColor(R.color.navigation_item_unselected));
            holder.tvName.setTextColor(context.getResources().getColor(R.color.navigation_item_unselected));
            holder.ivIcon.setImageResource(model.getResourceID());
        }
    }

    private void bindFollowUpHeader(@NonNull MyViewHolder holder) {
        View view = holder.getView();
        holder.tvName.setText(R.string.navigation_follow_up);
        view.setTag(null);
        view.setOnClickListener(null);
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription(context.getString(R.string.navigation_follow_up));
        ViewCompat.setAccessibilityHeading(view, true);
    }

    private void bindOtherModules(@NonNull MyViewHolder holder) {
        View view = holder.getView();
        boolean expanded = sections.isExpanded();
        holder.tvName.setText(R.string.navigation_other_modules);
        holder.ivIcon.setRotation(expanded ? 180 : 0);
        view.setTag(null);
        view.setContentDescription(context.getString(expanded
                ? R.string.navigation_other_modules_expanded
                : R.string.navigation_other_modules_collapsed));
        view.setOnClickListener(v -> {
            sections.setExpanded(!sections.isExpanded());
            notifyDataSetChanged();
        });
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Button");
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        sections.isExpanded() ? AccessibilityNodeInfoCompat.ACTION_COLLAPSE
                                : AccessibilityNodeInfoCompat.ACTION_EXPAND,
                        context.getString(sections.isExpanded()
                                ? R.string.navigation_collapse_other_modules
                                : R.string.navigation_expand_other_modules)));
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfoCompat.ACTION_EXPAND
                        || action == AccessibilityNodeInfoCompat.ACTION_COLLAPSE) {
                    sections.setExpanded(action == AccessibilityNodeInfoCompat.ACTION_EXPAND);
                    notifyDataSetChanged();
                    return true;
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        NavigationMenuSections.Row row = sections.getRows().get(position);
        return row.isFollowUpHeader() ? VIEW_TYPE_FOLLOW_UP_HEADER
                : row.isHeader() ? VIEW_TYPE_OTHER_MODULES : VIEW_TYPE_OPTION;
    }

    @Override
    public int getItemCount() {
        return sections.getRows().size();
    }

    public Map<String, Class> getRegisteredActivities() {
        return registeredActivities;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvCount;
        public ImageView ivIcon;

        private View myView;
        private final int paddingStart;
        private final int paddingEnd;

        private MyViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvName);
            tvCount = view.findViewById(R.id.tvCount);
            ivIcon = view.findViewById(R.id.ivIcon);
            paddingStart = view.getPaddingStart();
            paddingEnd = view.getPaddingEnd();

            myView = view;
        }

        public View getView() {
            return myView;
        }
    }

}
