package org.smartregister.chw.core.rule;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.smartregister.chw.core.utils.CoreConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AsrhFollowupRule implements ICommonRule {
    public static final String RULE_KEY = "asrhFollowupRule";
    private String visitID;
    private final DateTime nextAppointmentDate;
    private DateTime dueDate;
    private DateTime overDueDate;
    private DateTime expiryDate;
    private int daysDifference;

    public AsrhFollowupRule(Date appointmentDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.nextAppointmentDate = appointmentDate != null ? new DateTime(sdf.format(appointmentDate)) : null;
    }

    public String getVisitID() {
        return visitID;
    }

    public void setVisitID(String visitID) {
        this.visitID = visitID;
    }

    public int getDaysDifference() {
        return daysDifference;
    }

    public boolean isValid(int dueDay, int overdueDate, int expiry) {
        if (nextAppointmentDate != null) {
            this.dueDate = nextAppointmentDate.plusDays(dueDay);
            this.overDueDate = nextAppointmentDate.plusDays(overdueDate);
            this.expiryDate = nextAppointmentDate.plusDays(expiry);
        }

        daysDifference = Days.daysBetween(new DateTime(), new DateTime(dueDate)).getDays();
        return true;
    }

    public Date getDueDate() {
        return dueDate != null ? dueDate.toDate() : null;
    }

    public Date getOverDueDate() {
        return overDueDate != null ? overDueDate.toDate() : null;
    }

    public Date getExpiryDate() {
        return expiryDate != null ? expiryDate.toDate() : null;
    }

    @Override
    public String getRuleKey() {
        return "hivFollowupRule";
    }

    @Override
    public String getButtonStatus() {
        DateTime currentDate = new DateTime(new LocalDate().toDate());

        if (currentDate.isBefore(expiryDate)) {
            if ((currentDate.isAfter(overDueDate) || currentDate.isEqual(overDueDate)))
                return CoreConstants.VISIT_STATE.OVERDUE;
            if ((currentDate.isAfter(dueDate) || currentDate.isEqual(dueDate)) && currentDate.isBefore(overDueDate))
                return CoreConstants.VISIT_STATE.DUE;
            return CoreConstants.VISIT_STATE.NOT_DUE_YET;

        }
        return CoreConstants.VISIT_STATE.EXPIRED;
    }
}