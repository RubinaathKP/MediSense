package com.medisense.rag.reranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.reranking.exception.RerankerUnavailableException;
import com.medisense.rag.reranking.model.PassageDto;
import com.medisense.rag.reranking.model.RerankRequest;
import com.medisense.rag.reranking.model.RerankResponse;
import com.medisense.rag.reranking.model.RerankResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RerankingClientTest {

    private RerankingClient.HttpSender mockHttpSender;
    private HttpResponse<String> mockHttpResponse;
    private ObjectMapper objectMapper;
    private RerankerCircuitBreaker circuitBreaker;
    private RerankingClient client;

    @BeforeEach
    void setUp() {
        mockHttpSender = mock(RerankingClient.HttpSender.class);
        mockHttpResponse = mock(HttpResponse.class);
        objectMapper = new ObjectMapper();
        circuitBreaker = new RerankerCircuitBreaker();
        client = new RerankingClient("http://localhost:8001/rerank", circuitBreaker, objectMapper, mockHttpSender);
    }

    @Test
    void testSuccessfulRerankOnFirstAttempt() throws Exception {
        RerankResponse expectedResponse = new RerankResponse(List.of(
                new RerankResultDto("chunk-1", 0.95),
                new RerankResultDto("chunk-2", 0.85)
        ));

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(objectMapper.writeValueAsString(expectedResponse));
        when(mockHttpSender.send(any(HttpRequest.class))).thenReturn(mockHttpResponse);

        RerankRequest request = new RerankRequest("test query", List.of(new PassageDto("chunk-1", "text 1")), 5);
        RerankResponse actualResponse = client.rerank(request);

        assertNotNull(actualResponse);
        assertEquals(2, actualResponse.results().size());
        assertEquals("chunk-1", actualResponse.results().get(0).chunkId());
        assertEquals(0.95, actualResponse.results().get(0).score());
        assertEquals(RerankerCircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getConsecutiveFailures());
        verify(mockHttpSender, times(1)).send(any(HttpRequest.class));
    }

    @Test
    void testRetryAndSuccessOnThirdAttempt() throws Exception {
        RerankResponse expectedResponse = new RerankResponse(List.of(new RerankResultDto("chunk-1", 0.92)));

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(objectMapper.writeValueAsString(expectedResponse));

        when(mockHttpSender.send(any(HttpRequest.class)))
                .thenThrow(new IOException("Timeout 1"))
                .thenThrow(new IOException("Timeout 2"))
                .thenReturn(mockHttpResponse);

        RerankRequest request = new RerankRequest("test query", List.of(new PassageDto("chunk-1", "text 1")), 5);
        RerankResponse actualResponse = client.rerank(request);

        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.results().size());
        assertEquals(RerankerCircuitBreaker.State.CLOSED, circuitBreaker.getState());
        verify(mockHttpSender, times(3)).send(any(HttpRequest.class));
    }

    @Test
    void testExceedMaxRetriesTriggersCircuitBreakerFailureCount() throws Exception {
        when(mockHttpSender.send(any(HttpRequest.class)))
                .thenThrow(new IOException("Network failure"));

        RerankRequest request = new RerankRequest("test query", List.of(new PassageDto("chunk-1", "text 1")), 5);

        assertThrows(RerankerUnavailableException.class, () -> client.rerank(request));
        assertEquals(1, circuitBreaker.getConsecutiveFailures());
        verify(mockHttpSender, times(3)).send(any(HttpRequest.class));
    }

    @Test
    void testCircuitBreakerOpensAfterThreeConsecutiveFailuresAndFastFails() throws Exception {
        when(mockHttpSender.send(any(HttpRequest.class)))
                .thenThrow(new IOException("Service offline"));

        RerankRequest request = new RerankRequest("test query", List.of(new PassageDto("chunk-1", "text 1")), 5);

        // 3 consecutive failures
        assertThrows(RerankerUnavailableException.class, () -> client.rerank(request));
        assertThrows(RerankerUnavailableException.class, () -> client.rerank(request));
        assertThrows(RerankerUnavailableException.class, () -> client.rerank(request));

        assertEquals(3, circuitBreaker.getConsecutiveFailures());
        assertEquals(RerankerCircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 4th call should fast-fail via circuit breaker without executing HTTP call
        reset(mockHttpSender);
        RerankerUnavailableException ex = assertThrows(RerankerUnavailableException.class, () -> client.rerank(request));
        assertTrue(ex.getMessage().contains("Circuit breaker is OPEN"));
        verifyNoInteractions(mockHttpSender);
    }

    @Test
    void testCircuitBreakerResetsAfterTimeout() {
        Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(1000000L), ZoneId.of("UTC"));
        RerankerCircuitBreaker cb = new RerankerCircuitBreaker(fixedClock);
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(RerankerCircuitBreaker.State.OPEN, cb.getState());

        // Advance clock by 61 seconds (61,000 ms)
        Clock futureClock = Clock.fixed(Instant.ofEpochMilli(1000000L + 61000L), ZoneId.of("UTC"));
        cb.setClock(futureClock);

        assertTrue(cb.allowExecution());
        assertEquals(RerankerCircuitBreaker.State.CLOSED, cb.getState());
    }
}
