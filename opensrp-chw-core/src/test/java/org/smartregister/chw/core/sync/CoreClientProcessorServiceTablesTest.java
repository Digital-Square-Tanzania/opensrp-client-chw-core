package org.smartregister.chw.core.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.smartregister.domain.jsonmapping.Table;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE)
public class CoreClientProcessorServiceTablesTest {
    private RecordingTree logs;

    @Before
    public void setUp() {
        logs = new RecordingTree();
        Timber.plant(logs);
    }

    @After
    public void tearDown() {
        Timber.uproot(logs);
    }

    @Test
    public void singleObjectSupportsNamedAndDefaultLookupWithoutArrayParsingError() {
        TestProcessor processor =
                processor(
                        "{\"name\":\"vaccines\",\"columns\":[{\"column_name\":\"entity_id\","
                                + "\"json_mapping\":{\"field\":\"baseEntityId\"}}]}");

        Table table = processor.getServiceTable("vaccines");

        assertNotNull(table);
        assertEquals("vaccines", table.name);
        assertEquals("entity_id", table.columns.get(0).column_name);
        assertEquals("baseEntityId", table.columns.get(0).json_mapping.field);
        assertSame(table, processor.getServiceTable(null));
        assertSame(table, processor.getServiceTable(" "));
        assertSame(table, processor.getServiceTable("unknown"));
        assertEquals(1, processor.assetReads);
        assertTrue(logs.errors.isEmpty());
    }

    @Test
    public void tableArrayPreservesEachNamedMappingAndCachedFallback() {
        TestProcessor processor =
                processor(
                        "[{\"name\":\"vaccines\",\"columns\":[]},"
                                + "{\"name\":\"recurring_services\",\"columns\":[]}]");

        Table vaccines = processor.getServiceTable("vaccines");
        Table services = processor.getServiceTable("recurring_services");
        Table fallback = processor.getServiceTable(null);

        assertEquals("vaccines", vaccines.name);
        assertEquals("recurring_services", services.name);
        assertTrue(fallback == vaccines || fallback == services);
        assertSame(fallback, processor.getServiceTable("unknown"));
        assertEquals(1, processor.assetReads);
        assertTrue(logs.errors.isEmpty());
    }

    @Test
    public void arrayKeepsLegacyLastDefinitionForDuplicateNames() {
        TestProcessor processor =
                processor(
                        "[{\"name\":\"services\",\"columns\":[{\"column_name\":\"old\"}]},"
                                + "{\"name\":\"services\",\"columns\":[{\"column_name\":\"current\"}]}]");

        assertEquals(
                "current", processor.getServiceTable("services").columns.get(0).column_name);
        assertTrue(logs.errors.isEmpty());
    }

    @Test
    public void nullAndUnnamedArrayEntriesDoNotReplaceValidNamedMappings() {
        TestProcessor processor =
                processor(
                        "[null,{\"columns\":[]},{\"name\":\" \"},"
                                + "{\"name\":\"services\",\"columns\":[]}]");

        assertEquals("services", processor.getServiceTable("services").name);
        assertSame(processor.getServiceTable("services"), processor.getServiceTable("unknown"));
        assertTrue(logs.errors.isEmpty());
    }

    @Test
    public void emptyArrayRemainsEmptyAndIsNotReparsedForEveryLookup() {
        TestProcessor processor = processor("[]");

        assertNull(processor.getServiceTable(null));
        assertNull(processor.getServiceTable("unknown"));
        assertEquals(1, processor.assetReads);
        assertTrue(logs.errors.isEmpty());
    }

    @Test
    public void malformedJsonIsDiagnosedOnceWithoutASecondFormatAttempt() {
        TestProcessor processor = processor("{\"name\":");

        assertNull(processor.getServiceTable(null));
        assertNull(processor.getServiceTable("unknown"));
        assertEquals(1, processor.assetReads);
        assertDiagnostic();
    }

    @Test
    public void primitiveRootIsDiagnosedInsteadOfBeingUsedAsATable() {
        TestProcessor processor = processor("true");

        assertNull(processor.getServiceTable(null));
        assertDiagnostic();
    }

    @Test
    public void missingOrNullRootIsDiagnosedWithoutAFormatFallback() {
        for (String configuration : new String[] {null, "null"}) {
            logs.errors.clear();
            TestProcessor processor = processor(configuration);

            assertNull(processor.getServiceTable("services"));
            assertEquals(1, processor.assetReads);
            assertDiagnostic();
        }
    }

    @Test
    public void malformedArrayEntryCannotLeaveAPartiallyLoadedConfiguration() {
        TestProcessor processor =
                processor("[{\"name\":\"services\",\"columns\":[]},false]");

        assertNull(processor.getServiceTable("services"));
        assertNull(processor.getServiceTable(null));
        assertEquals(1, processor.assetReads);
        assertDiagnostic();
    }

    private TestProcessor processor(String configuration) {
        return new TestProcessor(configuration);
    }

    private void assertDiagnostic() {
        assertEquals(1, logs.errors.size());
        assertTrue(logs.errors.get(0).contains("Unable to parse service table configuration"));
        assertTrue(logs.errors.get(0).contains("ec_client_service.json"));
        assertFalse(logs.errors.get(0).isEmpty());
    }

    private static class TestProcessor extends CoreClientProcessor {
        private final String configuration;
        private int assetReads;

        TestProcessor(String configuration) {
            super(RuntimeEnvironment.application);
            this.configuration = configuration;
        }

        @Override
        protected <T> T assetJsonToJava(String fileName, Class<T> type) {
            assertEquals("ec_client_service.json", fileName);
            assertEquals(JsonElement.class, type);
            assetReads++;
            return configuration == null
                    ? null
                    : type.cast(new JsonParser().parse(configuration));
        }
    }

    private static class RecordingTree extends Timber.Tree {
        private final List<String> errors = new ArrayList<>();

        @Override
        protected void log(int priority, String tag, String message, Throwable throwable) {
            if (priority == android.util.Log.ERROR) {
                errors.add(message);
            }
        }
    }
}
