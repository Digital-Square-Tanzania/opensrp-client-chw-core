package org.smartregister.chw.core.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
 

import com.google.android.material.appbar.AppBarLayout;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;

import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.ChwNotificationDetailsContract;
import org.smartregister.chw.core.dao.ChwNotificationDao;
import org.smartregister.chw.core.domain.NotificationItem;
import org.smartregister.chw.core.presenter.BaseChwNotificationDetailsPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.MultiLanguageActivity;

import java.util.List;

import static org.smartregister.chw.core.utils.CoreConstants.DB_CONSTANTS.NOTIFICATION_ID;
import static org.smartregister.chw.core.utils.CoreConstants.DB_CONSTANTS.NOTIFICATION_TYPE;

public abstract class BaseChwNotificationDetailsActivity extends MultiLanguageActivity
        implements ChwNotificationDetailsContract.View, View.OnClickListener {

    protected TextView notificationTitle;
    protected TextView notificationDateTextView;
    protected TextView patientNameTextView;
    protected TextView reasonTextView;
    protected TextView notesTextView;
    protected View notesCardView;
    protected TextView notesToggleView;
    protected boolean notesExpanded = false;
    protected View notesRowView;
    protected View dateRowView;
    protected Button markAsDoneButton;
    protected Button viewProfileButton;
    protected ChwNotificationDetailsContract.Presenter presenter;
    protected String notificationId;
    protected String notificationType;
    protected CommonPersonObjectClient commonPersonObjectClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chw_notification_details);
        inflateToolbar();
        setupViews();
        setCommonPersonsObjectClient((CommonPersonObjectClient) getIntent().getSerializableExtra(CoreConstants.INTENT_KEY.CLIENT));
        initPresenter();
        disableMarkAsDoneAction(ChwNotificationDao.isMarkedAsDone(this, notificationId, notificationType));
    }

    private void inflateToolbar() {
        Toolbar toolbar = findViewById(R.id.back_to_updates_toolbar);
        // Title and navigation icon are defined in XML; just wire up back action
        toolbar.setNavigationOnClickListener(v -> finish());
        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        // Keep flat app bar appearance
        if (appBarLayout != null) {
            appBarLayout.setStateListAnimator(null);
        }
    }

    protected void setupViews() {
        notificationTitle = findViewById(R.id.notification_title);
        notificationDateTextView = findViewById(R.id.notification_date);
        patientNameTextView = findViewById(R.id.patient_name);
        reasonTextView = findViewById(R.id.text_reason);
        notesTextView = findViewById(R.id.text_notes);
        notesCardView = findViewById(R.id.notes_card);
        notesToggleView = findViewById(R.id.notes_toggle);
        notesRowView = findViewById(R.id.notes_row);
        dateRowView = findViewById(R.id.date_row);

        if (notesToggleView != null) {
            notesToggleView.setOnClickListener(v -> toggleNotes());
        }
        if (notesRowView != null) {
            notesRowView.setOnClickListener(v -> toggleNotes());
        }

        markAsDoneButton = findViewById(R.id.mark_as_done);
        if (markAsDoneButton != null) markAsDoneButton.setOnClickListener(this);

        viewProfileButton = findViewById(R.id.view_profile);
        if (viewProfileButton != null) viewProfileButton.setOnClickListener(this);
    }

    @Override
    public void setNotificationDetails(NotificationItem notificationItem) {
        // Header content
        if (isFacilityLinkageType()) {
            // Subject as static per new UI
            notificationTitle.setText("Linkage from Health Facility");
            // Patient name from client
            if (patientNameTextView != null && getCommonPersonObjectClient() != null) {
                String first = Utils.getValue(getCommonPersonObjectClient().getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
                String middle = Utils.getValue(getCommonPersonObjectClient().getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
                String last = Utils.getValue(getCommonPersonObjectClient().getColumnmaps(), DBConstants.KEY.LAST_NAME, true);
                String name = (first + " " + middle + " " + last).replaceAll("\\s+", " ").trim();
                patientNameTextView.setText(name);
            }

            // Try derive date from title: last token after " on "
            if (notificationDateTextView != null) {
                String date = extractDateFromTitle(notificationItem.getTitle());
                notificationDateTextView.setText(date);
                if (dateRowView != null) {
                    dateRowView.setVisibility(TextUtils.isEmpty(date) ? View.GONE : View.VISIBLE);
                }
            }

            // Reason content from details (take value after colon, if present)
            if (reasonTextView != null) {
                String reason = extractPrimaryDetailValue(notificationItem.getDetails());
                if (reason != null) {
                    reasonTextView.setText(reason);
                }
            }
        } else {
            // Fallback for other notification types: show title as header, and map first detail as body text
            notificationTitle.setText(notificationItem.getTitle());
            if (reasonTextView != null) {
                String value = extractPrimaryDetailValue(notificationItem.getDetails());
                if (value != null) reasonTextView.setText(value);
            }
            if (patientNameTextView != null) patientNameTextView.setVisibility(View.GONE);
            if (notificationDateTextView != null) notificationDateTextView.setVisibility(View.GONE);
            if (dateRowView != null) dateRowView.setVisibility(View.GONE);
            if (viewProfileButton != null) viewProfileButton.setVisibility(View.GONE);
        }

        // Notes handling for all types: use remaining details (excluding the first one used as reason)
        if (notesTextView != null && notesCardView != null) {
            String notes = extractNotes(notificationItem.getDetails());
            String reason = extractPrimaryDetailValue(notificationItem.getDetails());
            boolean isNewRegistration = reason != null && reason.equalsIgnoreCase("New registration");

            if ((notes == null || notes.isEmpty()) && (isFacilityLinkageType() || isNewRegistration)) {
                // Provide a placeholder guidance note for facility linkage new registration workflow
                notes = getString(R.string.new_registration_notes_placeholder);
            }

            if (notes != null && !notes.isEmpty()) {
                notesTextView.setText(notes);
                notesCardView.setVisibility(View.VISIBLE);
                configureNotesToggle();
            } else {
                notesCardView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void initPresenter() {
        presenter = new BaseChwNotificationDetailsPresenter(this);
        if (getIntent() != null && getIntent().getExtras() != null) {
            notificationId = getIntent().getExtras().getString(NOTIFICATION_ID);
            notificationType = getIntent().getExtras().getString(NOTIFICATION_TYPE);
            presenter.getNotificationDetails(notificationId, notificationType);
        }
    }

    @Override
    public void disableMarkAsDoneAction(boolean disable) {
        if (disable) {
            if (markAsDoneButton != null) {
                markAsDoneButton.setEnabled(false);
                markAsDoneButton.setClickable(false);
                markAsDoneButton.setAlpha(0.6f);
            }
        }
    }

    private String extractPrimaryDetailValue(List<String> details) {
        if (details == null || details.isEmpty()) return null;
        String first = details.get(0);
        int idx = first.indexOf(":");
        if (idx >= 0 && idx + 1 < first.length()) {
            return first.substring(idx + 1).trim();
        }
        return first;
    }

    private String extractNotes(List<String> details) {
        if (details == null || details.size() <= 1) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < details.size(); i++) {
            String v = details.get(i);
            int idx = v.indexOf(":");
            if (idx >= 0 && idx + 1 < v.length()) v = v.substring(idx + 1).trim();
            if (v == null || v.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(v);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void configureNotesToggle() {
        if (notesTextView == null || notesToggleView == null) return;
        // Reset to collapsed initially
        notesExpanded = false;
        notesTextView.setMaxLines(1);
        notesTextView.setEllipsize(TextUtils.TruncateAt.END);
        notesToggleView.setText(R.string.notes_more);

        // Show toggle whenever there is notes content so users can expand, even if not truncated
        notesTextView.post(() -> {
            boolean hasText = !TextUtils.isEmpty(notesTextView.getText());
            notesToggleView.setVisibility(hasText ? View.VISIBLE : View.GONE);
        });
    }

    private void toggleNotes() {
        if (notesTextView == null || notesToggleView == null) return;
        notesExpanded = !notesExpanded;
        if (notesExpanded) {
            notesTextView.setMaxLines(Integer.MAX_VALUE);
            notesTextView.setEllipsize(null);
            notesToggleView.setText(R.string.notes_less);
        } else {
            notesTextView.setMaxLines(1);
            notesTextView.setEllipsize(TextUtils.TruncateAt.END);
            notesToggleView.setText(R.string.notes_more);
        }
    }

    private boolean isFacilityLinkageType() {
        return notificationType != null && notificationType.equalsIgnoreCase(org.smartregister.chw.core.interactor.BaseChwNotificationDetailsInteractor.LINKAGE_FROM_FACILITY);
    }

    private String extractDateFromTitle(String title) {
        if (title == null) return "";
        // Try to find the last occurrence of " on " and take the remainder
        int marker = title.toLowerCase().lastIndexOf(" on ");
        if (marker >= 0 && marker + 4 < title.length()) {
            return title.substring(marker + 4).trim();
        }
        return "";
    }

    public ChwNotificationDetailsContract.Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void setCommonPersonsObjectClient(CommonPersonObjectClient client) {
        commonPersonObjectClient = client;
    }

    @Override
    public CommonPersonObjectClient getCommonPersonObjectClient() {
        return commonPersonObjectClient;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.view_profile) {
            goToMemberProfile();
        } else if (view.getId() == R.id.mark_as_done) {
            getPresenter().dismissNotification(notificationId, notificationType);
        } else {
            Utils.showShortToast(this, getString(R.string.perform_click_action));
        }
    }
}
