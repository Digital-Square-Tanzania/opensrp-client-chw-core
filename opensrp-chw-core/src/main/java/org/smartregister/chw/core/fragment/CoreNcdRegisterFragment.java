package org.smartregister.chw.core.fragment;

import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;

import org.smartregister.chw.agyw.presenter.BaseAGYWRegisterFragmentPresenter;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.model.CoreAgywRegisterFragmentModel;
import org.smartregister.chw.core.model.CoreNcdRegisterFragmentModel;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.ncd.fragment.BaseNcdRegisterFragment;
import org.smartregister.chw.ncd.presenter.BaseNcdRegisterFragmentPresenter;
import org.smartregister.view.customcontrols.CustomFontTextView;

import timber.log.Timber;

public class CoreNcdRegisterFragment extends BaseNcdRegisterFragment {

    protected Toolbar toolbar;
    @Override
    public void setupViews(View view) {
        super.setupViews(view);


        this.toolbar = (Toolbar) view.findViewById(org.smartregister.R.id.register_toolbar);
        this.toolbar.setContentInsetsAbsolute(0, 0);
        this.toolbar.setContentInsetsRelative(0, 0);
        this.toolbar.setContentInsetStartWithNavigation(0);

        View navbarContainer = view.findViewById(R.id.register_nav_bar_container);
        navbarContainer.setFocusable(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View searchBarLayout = view.findViewById(R.id.search_bar_layout);
        searchBarLayout.setLayoutParams(params);
        searchBarLayout.setBackgroundResource(R.color.chw_primary);
        searchBarLayout.setPadding(searchBarLayout.getPaddingLeft(), searchBarLayout.getPaddingTop(), searchBarLayout.getPaddingRight(), (int) Utils.convertDpToPixel(10, getActivity()));

        CustomFontTextView titleView = view.findViewById(R.id.txt_title_label);
        if (titleView != null) {
            titleView.setPadding(0, titleView.getTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());
        }

        if (getSearchView() != null) {
            getSearchView().setBackgroundResource(R.color.white);
            getSearchView().setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_search, 0, 0, 0);
            getSearchView().setTextColor(getResources().getColor(R.color.text_black));
        }

        View sortFilterBar = view.findViewById(org.smartregister.R.id.register_sort_filter_bar_layout);
        if (sortFilterBar != null) sortFilterBar.setVisibility(View.GONE);

        try {
            NavigationMenu.getInstance(getActivity(), null, toolbar);
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Timber.e(e);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            NavigationMenu.getInstance(getActivity(), null, null);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void initializePresenter() {
        presenter = new BaseNcdRegisterFragmentPresenter(this, new CoreNcdRegisterFragmentModel(), null);
    }

    @Override
    public void setTotalPatients() {
        // Do nothing, total patients is not required for NCD register
    }
}
