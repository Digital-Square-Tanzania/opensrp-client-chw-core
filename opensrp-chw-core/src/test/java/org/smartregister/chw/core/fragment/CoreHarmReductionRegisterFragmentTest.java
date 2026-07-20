package org.smartregister.chw.core.fragment;

import org.junit.Assert;
import org.junit.Test;

public class CoreHarmReductionRegisterFragmentTest {

    @Test
    public void searchFilterIncludesNicknameFromActiveRegisterTable() {
        String filter = CoreHarmReductionRegisterFragment.buildSearchFilter(
                "ec_harm_reduction_risk_assessment",
                "Mzee"
        );

        Assert.assertTrue(filter.contains("ec_harm_reduction_risk_assessment.nickname like ''%Mzee%''"));
    }

    @Test
    public void searchFilterIsEmptyWhenNoSearchTextWasEntered() {
        Assert.assertEquals("", CoreHarmReductionRegisterFragment.buildSearchFilter(
                "ec_harm_reduction_risk_assessment",
                ""
        ));
    }
}
