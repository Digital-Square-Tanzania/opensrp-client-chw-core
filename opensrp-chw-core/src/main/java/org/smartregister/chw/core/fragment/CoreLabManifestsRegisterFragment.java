package org.smartregister.chw.core.fragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.QueryBuilder;
import org.smartregister.chw.lab.fragment.BaseLabManifestsRegisterFragment;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.DBConstants;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;

import java.text.MessageFormat;
import java.util.List;

import timber.log.Timber;

public class CoreLabManifestsRegisterFragment extends BaseLabManifestsRegisterFragment {
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


    public String defaultFilterAndSortQuery() {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);
        String query = "";
        StringBuilder customFilter = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            customFilter.append(MessageFormat.format(" and ( {0}.{1} like ''%{2}%'' ", Constants.TABLES.LAB_MANIFESTS, DBConstants.KEY.BATCH_NUMBER, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ", Constants.TABLES.LAB_MANIFESTS, DBConstants.KEY.MANIFEST_TYPE, filters));
            customFilter.append(MessageFormat.format(" or {0}.{1} like ''%{2}%'' ) ", Constants.TABLES.LAB_MANIFESTS, DBConstants.KEY.SAMPLE_LIST, filters));
        }

        try {
            if (isValidFilterForFts(commonRepository())) {

                String myquery = QueryBuilder.getQuery(joinTables, mainCondition, tablename, customFilter.toString(), clientAdapter, Sortqueries);
                List<String> ids = commonRepository().findSearchIds(myquery);
                query = sqb.toStringFts(ids, tablename, CommonRepository.ID_COLUMN,
                        Sortqueries);
                query = sqb.Endquery(query);
            } else {
                sqb.addCondition(customFilter.toString());
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
            String query = mainSelect + presenter().getMainCondition();

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
