package org.smartregister.chw.core.fragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.QueryBuilder;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.lab.fragment.BaseLabRequestsRegisterFragment;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.DBConstants;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import java.text.MessageFormat;
import java.util.List;

import timber.log.Timber;

public class CoreLabRequestsRegisterFragment extends BaseLabRequestsRegisterFragment {
    private View view;

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        try {
            NavigationMenu.getInstance(getActivity(), null, toolbar);
            this.view = view;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            View searchBarLayout = view.findViewById(R.id.search_bar_layout);
            searchBarLayout.setLayoutParams(params);
            searchBarLayout.setBackgroundResource(R.color.chw_primary);
            searchBarLayout.setPadding(searchBarLayout.getPaddingLeft(), searchBarLayout.getPaddingTop(), searchBarLayout.getPaddingRight(), (int) Utils.convertDpToPixel(10, getActivity()));

            if (getSearchView() != null) {
                getSearchView().setBackgroundResource(org.smartregister.family.R.color.white);
                getSearchView().setCompoundDrawablesWithIntrinsicBounds(org.smartregister.family.R.drawable.ic_action_search, 0, 0, 0);
                getSearchView().setTextColor(getResources().getColor(R.color.text_black));
            }

        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = view.findViewById(org.smartregister.R.id.register_toolbar);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);
        NavigationMenu.getInstance(getActivity(), null, toolbar);
    }

    public String defaultFilterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);

        String query = "";
        StringBuilder customFilter = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", Constants.TABLES.LAB_TEST_REQUESTS, DBConstants.KEY.PATIENT_ID, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", Constants.TABLES.LAB_TEST_REQUESTS, DBConstants.KEY.SAMPLE_ID, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", Constants.TABLES.LAB_TEST_REQUESTS, DBConstants.KEY.SAMPLE_ID, filters));
        }

        try {
            if (isValidFilterForFts(commonRepository())) {

                String myquery = QueryBuilder.getQuery(joinTables, mainCondition, tablename, " AND " + customGroupFilter + customFilter, clientAdapter, Sortqueries);
                List<String> ids = commonRepository().findSearchIds(myquery);
                query = sqb.toStringFts(ids, tablename, CommonRepository.ID_COLUMN,
                        Sortqueries);
                query = sqb.Endquery(query);
            } else {
                sqb.addCondition(" AND " + customGroupFilter + customFilter);
                query = sqb.orderbyCondition(Sortqueries);
                query = sqb.Endquery(sqb.addlimitandOffset(query, clientAdapter.getCurrentlimit(), clientAdapter.getCurrentoffset()));

            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return query;
    }


    @Override
    public void countExecute() {
        Cursor c = null;
        try {
            String query = "select count(*) from " + presenter().getMainTable() + " inner join " + CoreConstants.TABLE_NAME.FAMILY_MEMBER +
                    " on " + presenter().getMainTable() + "." + DBConstants.KEY.BASE_ENTITY_ID + " = " +
                    CoreConstants.TABLE_NAME.FAMILY_MEMBER + "." + DBConstants.KEY.BASE_ENTITY_ID +
                    " where " + presenter().getMainCondition() + " AND " + customGroupFilter;

            if (StringUtils.isNotBlank(filters)) {

                StringBuilder customFilter = new StringBuilder();
                if (StringUtils.isNotBlank(filters)) {
                    customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", Constants.TABLES.LAB_TEST_REQUESTS, DBConstants.KEY.PATIENT_ID, filters));
                    customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", Constants.TABLES.LAB_TEST_REQUESTS, DBConstants.KEY.SAMPLE_ID, filters));
                    customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", Constants.TABLES.LAB_TEST_REQUESTS, DBConstants.KEY.SAMPLE_ID, filters));
                }

                query = query + " and ( " + customFilter + " ) ";
            }

            c = commonRepository().rawCustomQueryForAdapter(query);
            c.moveToFirst();
            clientAdapter.setTotalcount(c.getInt(0));
            Timber.v("total count here %s", clientAdapter.getTotalcount());

            clientAdapter.setCurrentlimit(20);
            clientAdapter.setCurrentoffset(0);

        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    @SuppressLint("StaticFieldLeak")
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        if (id == LOADER_ID) {
            return new CursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    // Count query
                    final String COUNT = "count_execute";
                    if (args != null && args.getBoolean(COUNT)) {
                        countExecute();
                    }
                    String query = defaultFilterAndSortQuery();
                    return commonRepository().rawCustomQueryForAdapter(query);
                }
            };
        }
        return super.onCreateLoader(id, args);
    }
}
