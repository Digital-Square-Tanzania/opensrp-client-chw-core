package org.smartregister.chw.core.interactor;

import org.junit.Assert;
import org.junit.Test;

public class NavigationInteractorQueryTest {

    @Test
    public void buildHarmReductionSoberHouseCountQueryShouldCountOnlyActiveClients() {
        String query = NavigationInteractor.buildHarmReductionSoberHouseCountQuery();

        Assert.assertTrue(query.contains("from ec_harm_reduction_sober_house_enrollment p"));
        Assert.assertTrue(query.contains("inner join ec_family_member m on p.base_entity_id = m.base_entity_id"));
        Assert.assertTrue(query.contains("m.date_removed is null"));
        Assert.assertTrue(query.contains("p.is_closed = 0 AND p.detoxification_done = 'yes'"));
        Assert.assertTrue(query.contains("SELECT s.follow_up_status FROM ec_harm_reduction_sober_house_services s"));
        Assert.assertTrue(query.contains("s.entity_id = p.base_entity_id AND s.is_closed = 0"));
        Assert.assertTrue(query.contains("ORDER BY s.last_interacted_with DESC LIMIT 1"));
        Assert.assertTrue(query.contains("'continuing_service') = 'continuing_service'"));
    }
}
