package org.smartregister.chw.core.listener;

import static org.smartregister.chw.core.utils.Utils.getClientName;

import android.app.Activity;
import android.database.Cursor;
import android.widget.Toast;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.domain.IndependentClientOption;
import org.smartregister.chw.core.fragment.AddExistingMemberFragment;
import org.smartregister.chw.core.fragment.AddMemberFragment;
import org.smartregister.chw.core.fragment.FamilyCallDialogFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class FloatingMenuListener implements OnClickFloatingMenu {
    private static FloatingMenuListener instance;
    private WeakReference<Activity> context;
    private String familyBaseEntityId;
    private static final String ENTITY_TYPE_INDEPENDENT_CLIENT = "ec_independent_client";


    private FloatingMenuListener(Activity context, String familyBaseEntityId) {
        this.context = new WeakReference<>(context);
        this.familyBaseEntityId = familyBaseEntityId;
    }

    public static FloatingMenuListener getInstance(Activity context, String familyBaseEntityId) {
        if (instance == null) {
            instance = new FloatingMenuListener(context, familyBaseEntityId);
        } else {
            instance.setFamilyBaseEntityId(familyBaseEntityId);
            if (instance.context.get() != context) {
                instance.context = new WeakReference<>(context);
            }
        }
        return instance;
    }

    public String getFamilyBaseEntityId() {
        return familyBaseEntityId;
    }

    public FloatingMenuListener setFamilyBaseEntityId(String familyBaseEntityId) {
        this.familyBaseEntityId = familyBaseEntityId;
        return this;
    }

    @Override
    public void onClickMenu(int viewId) {
        if (context.get() != null) {

            if (context.get().isDestroyed()) {
                Timber.d("Activity Destroyed");
                return;
            }

            if (viewId == R.id.call_layout) {
                FamilyCallDialogFragment.launchDialog(context.get(), familyBaseEntityId);
            } else if (viewId == R.id.add_new_member_layout) {
                AddMemberFragment addmemberFragment = AddMemberFragment.newInstance();
                addmemberFragment.setContext(context.get());
                addmemberFragment.show(context.get().getFragmentManager(), AddMemberFragment.DIALOG_TAG);
            } else if (viewId == R.id.add_existing_member_layout) {
                List<IndependentClientOption> independentClients = loadIndependentClients();
                if (independentClients.isEmpty()) {
                    Toast.makeText(context.get(), R.string.no_independent_clients_available, Toast.LENGTH_SHORT).show();
                } else {
                    AddExistingMemberFragment addExistingMemberFragment = AddExistingMemberFragment.newInstance(familyBaseEntityId, independentClients);
                    addExistingMemberFragment.setContext(context.get());
                    addExistingMemberFragment.show(context.get().getFragmentManager(), AddExistingMemberFragment.DIALOG_TAG);}
            }
        }
    }

    private List<IndependentClientOption> loadIndependentClients() {
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
}
