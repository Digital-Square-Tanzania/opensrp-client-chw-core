package org.smartregister.chw.core.provider;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import org.smartregister.provider.VmmcRegisterProvider;
import org.smartregister.view.contract.SmartRegisterClient;
import java.util.Set;

public class ChwVmmcRegisterProvider extends VmmcRegisterProvider {


    public ChwVmmcRegisterProvider(Context context, View.OnClickListener paginationClickListener,
                                   View.OnClickListener onClickListener, Set visibleColumns) {
        super(context, paginationClickListener, onClickListener, visibleColumns);
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);

        viewHolder.dueButton.setVisibility(View.GONE);
        viewHolder.dueButton.setOnClickListener(null);
    }
}
