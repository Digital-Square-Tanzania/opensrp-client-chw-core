package org.smartregister.chw.core.sync.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.chw.core.dao.ChwNotificationDao;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Response;
import org.smartregister.domain.ResponseStatus;
import org.smartregister.service.HTTPAgent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE,
        shadows = ChwCoreSyncIntentServiceRetryTest.NotificationDaoShadow.class)
public class ChwCoreSyncIntentServiceRetryTest {
    private HTTPAgent agent;
    private TestSyncService service;
    private String originalFailureDisplayValue;

    @Before
    public void setUp() {
        agent = mock(HTTPAgent.class);
        service = new TestSyncService(agent);
        NotificationDaoShadow.ids = Arrays.asList("linked-client-one", "linked-client-two");
        NotificationDaoShadow.reads = 0;
        originalFailureDisplayValue = FetchStatus.fetchedFailed.displayValue();
    }

    @After
    public void tearDown() {
        NotificationDaoShadow.ids = Collections.emptyList();
        FetchStatus.fetchedFailed.setDisplayValue(originalFailureDisplayValue);
    }

    @Test
    public void persistentHttpFailureStopsAfterFourAttemptsAndCompletesFailed() {
        when(agent.postWithJsonResponse(anyString(), anyString()))
                .thenReturn(new Response<>(ResponseStatus.failure, null));

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(4);
        assertEquals(4, NotificationDaoShadow.reads);
        assertEquals("[\"linked-client-one\",\"linked-client-two\"]", service.lastIds.toString());
    }

    @Test
    public void missingEventCountStopsAfterFourAttempts() {
        respondWith("{}");

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(4);
    }

    @Test
    public void negativeEventCountStopsAfterFourAttempts() {
        respondWith("{\"no_of_events\":-1}");

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(4);
    }

    @Test
    public void malformedResponseStopsAfterFourAttempts() {
        respondWith("not-json");

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(4);
    }

    @Test
    public void requestExceptionStopsAfterFourAttempts() {
        when(agent.postWithJsonResponse(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Offline request failure"));

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(4);
    }

    @Test
    public void processingExceptionStopsAfterFourAttempts() {
        respondWith("{\"no_of_events\":1}");
        service.failProcessing = true;

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(4);
        assertEquals(4, service.processingAttempts);
    }

    @Test
    public void retriesCanRecoverAndProcessTheSuccessfulResponseOnce() {
        when(agent.postWithJsonResponse(anyString(), anyString()))
                .thenReturn(new Response<>(ResponseStatus.failure, null))
                .thenReturn(new Response<>(ResponseStatus.success, "{}"))
                .thenReturn(new Response<>(ResponseStatus.success, "{\"no_of_events\":-1}"))
                .thenReturn(new Response<>(ResponseStatus.success, "{\"no_of_events\":2}"));

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        verify(agent, times(4)).postWithJsonResponse(anyString(), anyString());
        assertEquals(1, service.processingAttempts);
        assertNotNull(service.processed);
        assertEquals(2, service.processed.optInt("no_of_events"));
        assertEquals(0, service.completions);
    }

    @Test
    public void validEmptyResponseIsProcessedWithoutRetrying() {
        respondWith("{\"no_of_events\":0}");

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        verify(agent).postWithJsonResponse(anyString(), anyString());
        assertEquals(1, service.processingAttempts);
        assertEquals(0, service.completions);
    }

    @Test
    public void timeoutResponseCompletesFailedWithoutRetrying() {
        Response response = mock(Response.class);
        when(response.isTimeoutError()).thenReturn(true);
        when(response.status()).thenReturn(ResponseStatus.failure);
        when(agent.postWithJsonResponse(anyString(), anyString())).thenReturn(response);

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(1);
        assertEquals(ResponseStatus.failure.displayValue(), FetchStatus.fetchedFailed.displayValue());
    }

    @Test
    public void urlFailureCompletesFailedWithoutRetrying() {
        Response response = mock(Response.class);
        when(response.isUrlError()).thenReturn(true);
        when(response.status()).thenReturn(ResponseStatus.failure);
        when(agent.postWithJsonResponse(anyString(), anyString())).thenReturn(response);

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(1);
    }

    @Test
    public void missingHttpAgentCompletesFailedWithoutFetching() {
        service.agent = null;

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        assertFailedAfterAttempts(0);
    }

    @Test
    public void noMissingClientsDoesNotFetchOrPublishFailure() {
        NotificationDaoShadow.ids = Collections.emptyList();

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(0);

        verify(agent, never()).postWithJsonResponse(anyString(), anyString());
        assertEquals(0, service.completions);
        assertNull(service.processed);
    }

    @Test
    public void startingAtFinalRetryAllowsOnlyOneRequest() {
        when(agent.postWithJsonResponse(anyString(), anyString()))
                .thenReturn(new Response<>(ResponseStatus.failure, null));

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(3);

        assertFailedAfterAttempts(1);
    }

    @Test
    public void exhaustedRetryCountDoesNotIssueAnotherRequest() {
        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(4);

        assertFailedAfterAttempts(0);
        assertEquals(0, NotificationDaoShadow.reads);
    }

    @Test
    public void negativeStartingCountDoesNotExceedFourAttempts() {
        when(agent.postWithJsonResponse(anyString(), anyString()))
                .thenReturn(new Response<>(ResponseStatus.failure, null));

        service.fetchMissingEventsForLinkedClientsFromFacilityToCommunity(-1);

        assertFailedAfterAttempts(4);
    }

    private void respondWith(String payload) {
        when(agent.postWithJsonResponse(anyString(), anyString()))
                .thenReturn(new Response<>(ResponseStatus.success, payload));
    }

    private void assertFailedAfterAttempts(int attempts) {
        verify(agent, times(attempts)).postWithJsonResponse(anyString(), anyString());
        assertEquals(1, service.completions);
        assertEquals(FetchStatus.fetchedFailed, service.completedStatus);
        assertNull(service.processed);
    }

    @Implements(ChwNotificationDao.class)
    public static class NotificationDaoShadow {
        static List<String> ids;
        static int reads;

        @Implementation
        public static List<String> getBaseEntityIdsForLinkedClientsFromFacilityToCommunityWithMissingClientDetails() {
            reads++;
            return ids;
        }
    }

    private static class TestSyncService extends ChwCoreSyncIntentService {
        HTTPAgent agent;
        JSONArray lastIds;
        JSONObject processed;
        FetchStatus completedStatus;
        int processingAttempts;
        int completions;
        boolean failProcessing;

        TestSyncService(HTTPAgent agent) {
            super("linked-client-retry-test");
            this.agent = agent;
        }

        @Override
        public HTTPAgent getHttpAgent() {
            return agent;
        }

        @Override
        public Response fetchClientEventsByBaseEntityIds(JSONArray ids) {
            lastIds = ids;
            return agent.postWithJsonResponse("https://example.invalid/sync", ids.toString());
        }

        @Override
        protected void processClientEvent(JSONObject payload) {
            processingAttempts++;
            if (failProcessing) throw new IllegalStateException("Processing failure");
            processed = payload;
        }

        @Override
        protected void complete(FetchStatus status) {
            completedStatus = status;
            completions++;
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            // The tests call the actual linked-client retry method directly.
        }
    }
}
