package org.smartregister.chw.core.sync;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.core.BaseUnitTest;

public class CoreClientProcessorTest extends BaseUnitTest {

    @Test
    public void resolveUsedNeedlesAndSyringesCollectedShouldPreferCurrentField() {
        String value = CoreClientProcessor.resolveUsedNeedlesAndSyringesCollected(
                "14",
                "9",
                "4",
                "5"
        );

        Assert.assertEquals("14", value);
    }

    @Test
    public void resolveUsedNeedlesAndSyringesCollectedShouldFallBackToLegacyTotal() {
        String value = CoreClientProcessor.resolveUsedNeedlesAndSyringesCollected(
                "",
                "9",
                "4",
                "5"
        );

        Assert.assertEquals("9", value);
    }

    @Test
    public void resolveUsedNeedlesAndSyringesCollectedShouldComputeLegacyComponentTotal() {
        String value = CoreClientProcessor.resolveUsedNeedlesAndSyringesCollected(
                null,
                null,
                "4",
                "5"
        );

        Assert.assertEquals("9", value);
    }
}
