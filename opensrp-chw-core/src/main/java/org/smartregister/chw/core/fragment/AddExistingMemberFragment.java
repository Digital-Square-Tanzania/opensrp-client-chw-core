package org.smartregister.chw.core.fragment;

import static org.smartregister.chw.core.utils.Utils.getClientName;
import static org.smartregister.chw.core.utils.Utils.updateClientFamilyRelationship;

import android.app.DialogFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.utils.CoreConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class AddExistingMemberFragment extends DialogFragment {

    public static final String DIALOG_TAG = "add_existing_member_dialog";
    private static final String ENTITY_TYPE_INDEPENDENT_CLIENT = "ec_independent_client";

    private List<IndependentClientOption> independentClients = new ArrayList<>();

    private OnClientSelectedListener listener;

    private  String familyBaseEntityId;
    public interface OnClientSelectedListener {
        void onClientSelected(IndependentClientOption client);
    }

    public void setOnClientSelectedListener(OnClientSelectedListener listener) {
        this.listener = listener;
    }

    public static AddExistingMemberFragment newInstance(String familyBaseEntityId) {

        Bundle args = new Bundle();
        args.putString("family_id", familyBaseEntityId);
        AddExistingMemberFragment fragment = new AddExistingMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }


    private  List<IndependentClientOption> loadIndependentClients() {
        Cursor cursor = null;

        List<IndependentClientOption> independentClients = new ArrayList<>();
        try {
            SQLiteDatabase readableDatabase = CoreChwApplication.getInstance().getRepository().getReadableDatabase();
            if (readableDatabase == null) {
                return Collections.emptyList();
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT m.base_entity_id, m.first_name, m.middle_name, m.last_name, m.dob, m.unique_id ");
            sqlBuilder.append("FROM ec_family_member m ");
            sqlBuilder.append("INNER JOIN ec_family f ON f.base_entity_id = m.relational_id ");
            sqlBuilder.append("WHERE m.is_closed = 0 ");
            sqlBuilder.append("AND m.date_removed IS NULL ");
            sqlBuilder.append("AND m.dod IS NULL ");
            sqlBuilder.append("AND f.entity_type = ? ");

            List<String> args = new ArrayList<>();
            args.add(ENTITY_TYPE_INDEPENDENT_CLIENT);

            if (StringUtils.isNotBlank(familyBaseEntityId)) {
                sqlBuilder.append("AND m.relational_id <> ? ");
                args.add(familyBaseEntityId);
            }

            sqlBuilder.append("ORDER BY m.first_name, m.middle_name, m.last_name");

            cursor = readableDatabase.rawQuery(sqlBuilder.toString(), args.toArray(new String[0]));
            while (cursor != null && cursor.moveToNext()) {
                String baseEntityId = cursor.getString(0);
                if (StringUtils.isBlank(baseEntityId)) {
                    continue;
                }

                String firstName = cursor.getString(1);
                String middleName = cursor.getString(2);
                String lastName = cursor.getString(3);
                String dob = cursor.getString(4);
                String uniqueId = cursor.getString(5);

                String displayName = buildDisplayName(firstName, middleName, lastName, baseEntityId);
                String ageDisplay = buildAgeDisplay(dob);
                independentClients.add(new IndependentClientOption(baseEntityId, displayName, ageDisplay, uniqueId));
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to load independent clients for family reassignment");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return independentClients;
    }

    private static String buildDisplayName(String firstName, String middleName, String lastName, String fallback) {
        String clientName = getClientName(
                StringUtils.defaultString(firstName),
                StringUtils.defaultString(middleName),
                StringUtils.defaultString(lastName)
        );
        return StringUtils.isNotBlank(clientName) ? clientName : fallback;
    }

    private static String buildAgeDisplay(String dob) {
        if (StringUtils.isBlank(dob)) {
            return "";
        }
        try {
            DateTime birthDate = new DateTime(dob);
            int age = Math.max(0, new Period(birthDate, DateTime.now()).getYears());
            return String.format(Locale.getDefault(), "%d", age);
        } catch (Exception e) {
            Timber.w(e, "Unable to parse date of birth while loading independent clients");
            return "";
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.familyBaseEntityId = getArguments().getString("family_id");
        independentClients = loadIndependentClients();

        setStyle(DialogFragment.STYLE_NORMAL,
                android.R.style.Theme_Holo_Light_NoActionBar);
    }
    @Override
    public void onStart() {
        super.onStart();

        // Fullscreen dialog
        new Handler().post(() ->
                getDialog().getWindow().setLayout(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
    }
    private void reassignClientFamily(IndependentClientOption client) {
        if (client == null || StringUtils.isBlank(client.getBaseEntityId()) || StringUtils.isBlank(familyBaseEntityId)) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.unable_to_add_family_member), Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean updated = updateClientFamilyRelationship(
                    client.getBaseEntityId(),
                    familyBaseEntityId
            );

            getActivity().runOnUiThread(() -> {
                if (updated) {
                    Toast.makeText(
                            getActivity().getApplicationContext(),
                            getString(R.string.successfull_added_family_member, client.getDisplayName()),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.unable_to_add_family_member), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(
                R.layout.fragment_add_existing_member,
                container,
                false
        );

        EditText searchInput = rootView.findViewById(R.id.search_input);
        RecyclerView recyclerView = rootView.findViewById(R.id.contact_list);
        TextView emptyView = rootView.findViewById(R.id.empty_view);

        searchInput.setHint(R.string.client_search_hint);
        emptyView.setText(R.string.client_no_match);

        if (independentClients == null || independentClients.isEmpty()) {
            Toast.makeText(
                    getActivity(),
                    getString(R.string.no_independent_clients_available),
                    Toast.LENGTH_SHORT
            ).show();

            dismiss();
            return rootView;
        }

        IndependentClientSelectionAdapter adapter = new IndependentClientSelectionAdapter(independentClients);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        updateEmptyView(adapter, emptyView);

        adapter.setOnClientSelectedListener(client -> {
            if (listener != null) {
                listener.onClientSelected(client);
            }

            reassignClientFamily(client);

            if (getActivity() != null) {
                getActivity().setResult(CoreConstants.ProfileActivityResults.CHANGE_COMPLETED);
            }
            dismiss();

        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                adapter.filter(s == null ? "" : s.toString());
                updateEmptyView(adapter, emptyView);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        return rootView;
    }
    private void updateEmptyView(IndependentClientSelectionAdapter adapter, TextView emptyView) {
        emptyView.setVisibility(adapter.getItemCount() == 0
                ? View.VISIBLE
                : View.GONE);
    }
    private class IndependentClientSelectionAdapter extends RecyclerView.Adapter<IndependentClientViewHolder> {
        private final List<IndependentClientOption> allClients;
        private final List<IndependentClientOption> filteredClients = new ArrayList<>();
        private OnIndependentClientSelectedListener onClientSelectedListener;
        IndependentClientSelectionAdapter(List<IndependentClientOption> clients) {
            this.allClients = clients == null
                    ? Collections.emptyList()
                    : clients;
            this.filteredClients.addAll(this.allClients);
        }
        void setOnClientSelectedListener(OnIndependentClientSelectedListener listener) {
            this.onClientSelectedListener = listener;
        }
        void filter(String query) {
            filteredClients.clear();

            for (IndependentClientOption client : allClients) {
                if (client.matches(query)) {
                    filteredClients.add(client);
                }
            }

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public IndependentClientViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.independent_client_item_view, parent, false);

            return new IndependentClientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull IndependentClientViewHolder holder,
                int position
        ) {

            IndependentClientOption client = filteredClients.get(position);

            holder.nameView.setText(client.getDisplayName());

            String details = client.getDetails();
            if (StringUtils.isNotBlank(details)) {
                holder.detailsView.setText(details);
                holder.detailsView.setVisibility(View.VISIBLE);
            } else {
                holder.detailsView.setVisibility(View.GONE);
            }

            // ✅ Highlight + Disable Assigned Clients
//            if (client.isAssignedToHousehold()) {
//
//                holder.itemView.setAlpha(0.4f);
//                holder.itemView.setEnabled(false);
//
//            } else {
//
//                holder.itemView.setAlpha(1f);
//                holder.itemView.setEnabled(true);
//
//                holder.itemView.setOnClickListener(v -> {
//                    if (onClientSelectedListener != null) {
//                        onClientSelectedListener.onClientSelected(client);
//                    }
//                });
//            }

            holder.itemView.setAlpha(1f);
            holder.itemView.setEnabled(true);

            holder.itemView.setOnClickListener(v -> {
                if (onClientSelectedListener != null) {
                    onClientSelectedListener.onClientSelected(client);
                }
            });

        }

        @Override
        public int getItemCount() {
            return filteredClients.size();
        }
    }
    private interface OnIndependentClientSelectedListener {
        void onClientSelected(IndependentClientOption client);
    }
    private static class IndependentClientViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView detailsView;

        IndependentClientViewHolder(@NonNull View itemView) {
            super(itemView);

            nameView = itemView.findViewById(R.id.contact_name);
            detailsView = itemView.findViewById(R.id.contact_details);
        }
    }
    private static class IndependentClientOption {
        private final String baseEntityId;
        private final String displayName;
        private final String ageDisplay;
        private final String uniqueId;

        private IndependentClientOption(String baseEntityId, String displayName, String ageDisplay, String uniqueId) {
            this.baseEntityId = baseEntityId;
            this.displayName = displayName;
            this.ageDisplay = ageDisplay;
            this.uniqueId = uniqueId;
        }
        private String getBaseEntityId() {
            return baseEntityId;
        }
        private String getDisplayName() {
            return displayName;
        }
        private String getDetails() {
            StringBuilder detailsBuilder = new StringBuilder();
            if (StringUtils.isNotBlank(uniqueId)) {
                detailsBuilder.append(uniqueId);
            }
            if (StringUtils.isNotBlank(ageDisplay)) {
                if (detailsBuilder.length() > 0) {
                    detailsBuilder.append(" · ");
                }
                detailsBuilder.append(ageDisplay);
            }
            return detailsBuilder.toString();
        }
        private boolean matches(String query) {
            if (StringUtils.isBlank(query)) {
                return true;
            }
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            return (displayName != null && displayName.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                    || (uniqueId != null && uniqueId.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                    || (baseEntityId != null && baseEntityId.toLowerCase(Locale.getDefault()).contains(lowerQuery));
        }
    }
}
