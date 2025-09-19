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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View searchBarLayout = view.findViewById(R.id.search_bar_layout);
        searchBarLayout.setLayoutParams(params);
        searchBarLayout.setBackgroundResource(R.color.chw_primary);
        searchBarLayout.setPadding(searchBarLayout.getPaddingLeft(), searchBarLayout.getPaddingTop(), searchBarLayout.getPaddingRight(), (int) Utils.convertDpToPixel(10, getActivity()));

        try {
            NavigationMenu.getInstance(getActivity(), null, toolbar);
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
}
