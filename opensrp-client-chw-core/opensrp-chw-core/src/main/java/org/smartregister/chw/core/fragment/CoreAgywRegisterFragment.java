package org.smartregister.chw.core.fragment;

import android.view.View;

import org.smartregister.chw.agyw.fragment.BaseAGYWRegisterFragment;
import org.smartregister.chw.agyw.presenter.BaseAGYWRegisterFragmentPresenter;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.model.CoreAgywRegisterFragmentModel;

import timber.log.Timber;

public class CoreAgywRegisterFragment extends BaseAGYWRegisterFragment {
    @Override
    public void setupViews(View view) {
        super.setupViews(view);
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
            NavigationMenu.getInstance(getActivity(), null, toolbar);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void initializePresenter() {
        presenter = new BaseAGYWRegisterFragmentPresenter(this, new CoreAgywRegisterFragmentModel(), null);
    }
}
