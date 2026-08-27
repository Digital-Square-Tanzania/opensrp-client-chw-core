package org.smartregister.chw.core.dao;

import org.junit.Assert;
import org.junit.Test;

public class ChwNotificationDaoQueryTest {

    @Test
    public void filtersOpenFamilyPlanningRecordsByClient() {
        String query = ChwNotificationDao.getClientNotificationsQuery("client-id");

        Assert.assertTrue(query.contains(
                "SELECT id as notification_id, 'Family Planning' as notification_type\n" +
                        "FROM ec_family_planning_update\n" +
                        "WHERE entity_id = 'client-id' COLLATE NOCASE AND is_closed = 0"));
    }
}
