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

    @Test
    public void buildAypOutSchoolCountQueryShouldCountEligibleLivingClients() {
        String query = NavigationInteractor.buildAypOutSchoolCountQuery();

        Assert.assertTrue(query.contains("from ec_ayp_out_school_enrollment p"));
        Assert.assertTrue(query.contains("p.base_entity_id = ec_family_member.base_entity_id"));
        Assert.assertTrue(query.contains("p.is_closed is 0"));
        Assert.assertTrue(query.contains("ec_family_member.dod is null"));
        Assert.assertTrue(query.contains("should_enroll = 'yes'"));
    }

    @Test
    public void buildMotherMentorCountQueryShouldUseScreeningRegisterRows() {
        String query = NavigationInteractor.buildMotherMentorCountQuery();

        Assert.assertTrue(query.contains("from ec_mothermentor_screening p"));
        Assert.assertTrue(query.contains("p.base_entity_id = ec_family_member.base_entity_id"));
        Assert.assertTrue(query.contains("p.is_closed is 0"));
        Assert.assertTrue(query.contains("ec_family_member.dod is null"));
        Assert.assertTrue(query.contains("(p.status IS NULL OR p.status = 'client')"));
        Assert.assertTrue(query.contains("p.screening_status != '-'"));
    }
}
