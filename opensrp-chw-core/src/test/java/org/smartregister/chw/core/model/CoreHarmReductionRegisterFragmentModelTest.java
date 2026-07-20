package org.smartregister.chw.core.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class CoreHarmReductionRegisterFragmentModelTest {

    @Test
    public void mainColumnsIncludeNicknameFromActiveRegisterTable() {
        String tableName = "ec_harm_reduction_risk_assessment";

        String[] columns = new CoreHarmReductionRegisterFragmentModel().mainColumns(tableName);

        Assert.assertTrue(Arrays.asList(columns).contains(tableName + ".nickname"));
    }
}
