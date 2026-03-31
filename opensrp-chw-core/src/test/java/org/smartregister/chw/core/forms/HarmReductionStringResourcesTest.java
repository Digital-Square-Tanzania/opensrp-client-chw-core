package org.smartregister.chw.core.forms;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HarmReductionStringResourcesTest {

    @Test
    public void testSwahiliEnrollmentStringUsesKupunguza() throws Exception {
        String stringsXml = readText("src/main/res/values-sw/strings.xml");

        Assert.assertTrue(stringsXml.contains("<string name=\"harm_reduction_risk_assessment\">Usajili wa huduma za kupunguza madhara ya madawa ya kulevya</string>"));
        Assert.assertFalse(stringsXml.contains("Usajili wa huduma za kunguza madhara ya madawa ya kulevya"));
    }

    private static String readText(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolvePath(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path resolvePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }

        Path modulePath = Paths.get("opensrp-chw-core").resolve(relativePath);
        if (Files.exists(modulePath)) {
            return modulePath;
        }

        throw new AssertionError("Could not resolve path: " + relativePath);
    }
}
