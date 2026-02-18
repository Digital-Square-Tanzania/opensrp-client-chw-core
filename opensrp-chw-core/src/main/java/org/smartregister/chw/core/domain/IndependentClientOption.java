package org.smartregister.chw.core.domain;

import org.apache.commons.lang3.StringUtils;
import java.util.Locale;
import android.os.Parcel;
import android.os.Parcelable;

public class IndependentClientOption implements Parcelable {

    private final String baseEntityId;
    private final String displayName;
    private final String ageDisplay;
    private final String uniqueId;

    public IndependentClientOption(String baseEntityId, String displayName, String ageDisplay, String uniqueId) {
        this.baseEntityId = baseEntityId;
        this.displayName = displayName;
        this.ageDisplay = ageDisplay;
        this.uniqueId = uniqueId;
    }

    protected IndependentClientOption(Parcel in) {
        baseEntityId = in.readString();
        displayName = in.readString();
        ageDisplay = in.readString();
        uniqueId = in.readString();
    }


    public static final Creator<IndependentClientOption> CREATOR =
            new Creator<>() {
                @Override
                public IndependentClientOption createFromParcel(Parcel in) {
                    return new IndependentClientOption(in);
                }

                @Override
                public IndependentClientOption[] newArray(int size) {
                    return new IndependentClientOption[size];
                }
            };

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getAgeDisplay() {
        return ageDisplay;
    }

    public String getDetails() {
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

    public boolean matches(String query) {
        if (StringUtils.isBlank(query)) {
            return true;
        }

        String lowerQuery = query.toLowerCase(Locale.getDefault());

        return (displayName != null && displayName.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                || (uniqueId != null && uniqueId.toLowerCase(Locale.getDefault()).contains(lowerQuery))
                || (baseEntityId != null && baseEntityId.toLowerCase(Locale.getDefault()).contains(lowerQuery));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(baseEntityId);
        dest.writeString(displayName);
        dest.writeString(ageDisplay);
        dest.writeString(uniqueId);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

