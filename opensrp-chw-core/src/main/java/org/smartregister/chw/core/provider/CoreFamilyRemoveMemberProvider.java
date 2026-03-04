package org.smartregister.chw.core.provider;

import static org.smartregister.chw.core.utils.Utils.reprocessRegistrationEvents;
import static org.smartregister.chw.core.utils.Utils.updateClientFamilyRelationship;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.contract.FamilyRemoveMemberContract;
import org.smartregister.chw.core.interactor.CoreFamilyRemoveMemberInteractor;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.provider.FamilyMemberRegisterProvider;
import org.smartregister.view.customcontrols.CustomFontTextView;
import org.smartregister.view.customcontrols.FontVariant;

import java.util.HashMap;
import java.util.Set;

public abstract class CoreFamilyRemoveMemberProvider extends FamilyMemberRegisterProvider {

    private Context context;
    private View.OnClickListener footerClickListener;
    private String familyID;
    public static final String REMOVAL_REASON_START_NEW_FAMILY = "start_new_family";
    public static final String REMOVAL_REASON_CHANGE_TO_INDEPENDENT_CLIENT = "change_to_independent_client";
    public static final String REMOVAL_REASON_DEATH = "Death";


    public CoreFamilyRemoveMemberProvider(String familyID, Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener, String familyHead, String primaryCaregiver) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener, familyHead, primaryCaregiver);
        this.familyID = familyID;
        this.context = context;
        this.footerClickListener = paginationClickListener;
    }

    @Override
    public void getFooterView(RecyclerView.ViewHolder viewHolder, final int currentPageCount, final int totalPageCount, boolean hasNext, boolean hasPrevious) {
        // do nothing
        CoreFamilyRemoveMemberInteractor familyRemoveMemberInteractor = getFamilyRemoveMemberInteractor();
        final RemoveFooterViewHolder footerViewHolder = (RemoveFooterViewHolder) viewHolder;
        familyRemoveMemberInteractor.getFamilySummary(familyID, new FamilyRemoveMemberContract.InteractorCallback<>() {
            @Override
            public void onResult(HashMap<String, String> result) {
                Integer children = Integer.valueOf(result.get(CoreConstants.TABLE_NAME.CHILD));
                Integer members = Integer.valueOf(result.get(CoreConstants.TABLE_NAME.FAMILY_MEMBER));

                int adults = members - children;

                HashMap<String, String> payload = new HashMap<>();
                payload.put(CoreConstants.GLOBAL.MESSAGE, String.format(context.getString(R.string.remove_family_count), String.valueOf(adults), String.valueOf(children)));
                payload.put(CoreConstants.GLOBAL.NAME, result.get(CoreConstants.GLOBAL.NAME));

                footerViewHolder.instructions.setFontVariant(FontVariant.REGULAR);
                footerViewHolder.instructions.setTextColor(Color.BLACK);

                footerViewHolder.hint.setText(payload.get(CoreConstants.GLOBAL.MESSAGE));
                footerViewHolder.hint.setFontVariant(FontVariant.LIGHT);
                footerViewHolder.hint.setTextColor(Color.GRAY);
                footerViewHolder.hint.setTypeface(footerViewHolder.hint.getTypeface(), Typeface.NORMAL);

                footerViewHolder.view.setTag(payload);
            }

            @Override
            public void onError(Exception e) {
                //// TODO: 15/08/19
            }
            @Override
            public void onNewFamilyRegistrationSaved(String clientBaseEntityId, String familyBaseEntityId, String reasonForRemove) {
                if (reasonForRemove != null && reasonForRemove.equalsIgnoreCase(REMOVAL_REASON_START_NEW_FAMILY)) {
                    updateClientFamilyRelationship(clientBaseEntityId, familyBaseEntityId);
                } else if (reasonForRemove != null && reasonForRemove.equalsIgnoreCase(REMOVAL_REASON_CHANGE_TO_INDEPENDENT_CLIENT)) {
                    updateClientFamilyRelationship(clientBaseEntityId, familyBaseEntityId);
                    reprocessRegistrationEvents(familyBaseEntityId, clientBaseEntityId);
                }
            }

            @Override
            public void onUniqueIdFetched(Triple<String, String, String> triple, String entityId) {}
        });

        footerViewHolder.view.setOnClickListener(footerClickListener);
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup parent) {
        View view = inflater().inflate(R.layout.family_remove_member_footer, parent, false);
        view.findViewById(R.id.top).setVisibility(View.GONE);
        return new RemoveFooterViewHolder(view);
    }

    protected abstract CoreFamilyRemoveMemberInteractor getFamilyRemoveMemberInteractor();

    public class RemoveFooterViewHolder extends FooterViewHolder {
        public CustomFontTextView hint;
        public CustomFontTextView instructions;
        public View view;

        public RemoveFooterViewHolder(View view) {
            super(view);
            this.hint = view.findViewById(R.id.hint);
            this.instructions = view.findViewById(R.id.instructions);
            this.view = view;

            hint.setFontVariant(FontVariant.REGULAR);
        }
    }
}

