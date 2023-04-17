package org.smartregister.chw.core.interactor;

import org.smartregister.chw.core.contract.CoreVmmcProfileContract;
import org.smartregister.chw.core.dao.AlertDao;
import org.smartregister.chw.vmmc.interactor.BaseVmmcProfileInteractor;
import org.smartregister.domain.Alert;

import java.util.List;

public class CoreVmmcProfileInteractor extends BaseVmmcProfileInteractor implements CoreVmmcProfileContract.Interactor {

    private Alert getLatestAlert(String baseEntityID) {
        List<Alert> alerts = AlertDao.getActiveAlertsForVaccines(baseEntityID);

        if (alerts.size() > 0)
            return alerts.get(0);

        return null;
    }

}
