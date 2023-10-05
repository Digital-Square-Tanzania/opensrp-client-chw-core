package org.smartregister.chw.core.fragment;

import android.view.View;

import org.smartregister.chw.cdp.fragment.BaseCdpRegisterFragment;
import org.smartregister.chw.core.custom_views.NavigationMenu;

import timber.log.Timber;

public class CoreCdpRegisterFragment extends BaseCdpRegisterFragment {

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
}
