package com.medisense.unit.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.medisense.UnitTestBase;
import com.medisense.rag.reranking.RerankerCircuitBreaker;
import com.medisense.rag.reranking.RerankingClient;
import com.medisense.rag.reranking.RerankingOrchestrator;
import com.medisense.rag.reranking.exception.RerankerUnavailableException;
import com.medisense.rag.reranking.model.RerankResponse;
import com.medisense.rag.reranking.model.RerankResultDto;
import com.medisense.rag.reranking.model.RerankedChunk;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Test: RerankerClientTest
 * Tests WireMock HTTP integration, 3s timeout handling with RRF fallback,
 * 3-failure circuit breaker tripping, and 60s recovery window.
 */
class RerankerClientTest extends UnitTestBase {

    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper;
    private MutableClock testClock;
    private RerankerCircuitBreaker circuitBreaker;
    private RerankingClient rerankerClient;
    private RerankingOrchestrator orchestrator;

    private static class MutableClock extends Clock {
        private final AtomicLong millis;

        public MutableClock(long startMillis) {
            this.millis = new AtomicLong(startMillis);
        }

        public void advanceSeconds(long seconds) {
            this.millis.addAndGet(seconds * 1000L);
        }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
        @Override public long millis() { return millis.get(); }
    }

    @BeforeEach
    void initWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        objectMapper = new ObjectMapper();
        testClock = new MutableClock(System.currentTimeMillis());
        circuitBreaker = new RerankerCircuitBreaker(testClock);

        String mockUrl = "http://localhost:" + wireMockServer.port() + "/rerank";
        rerankerClient = new RerankingClient(mockUrl, circuitBreaker, objectMapper);
        orchestrator = new RerankingOrchestrator(rerankerClient);
    }

    @AfterEach
    void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private List<ScoredChunk> createCandidateChunks(int count) {
        List<ScoredChunk> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new ScoredChunk(
                    UUID.randomUUID(),
                    "Heading " + i,
                    "Passage " + i + " content regarding clinical evaluation.",
                    0.9 - (i * 0.05),
                    i,
                    "HYBRID"
            ));
        }
        return list;
    }

    @Test
    @DisplayName("Case 1: Successful rerank: mock response returns top-5 reranked results in order with scores")
    void testSuccessfulRerankReturnsTop5InOrder() throws Exception {
        List<ScoredChunk> candidates = createCandidateChunks(5);
        String query = "what does borderline LDL mean";

        // Build mock response where chunk 3 is top, then chunk 1, chunk 2, chunk 5, chunk 4
        List<RerankResultDto> mockResults = List.of(
                new RerankResultDto(candidates.get(2).chunkId().toString(), 0.985, 1),
                new RerankResultDto(candidates.get(0).chunkId().toString(), 0.920, 2),
                new RerankResultDto(candidates.get(1).chunkId().toString(), 0.850, 3),
                new RerankResultDto(candidates.get(4).chunkId().toString(), 0.760, 4),
                new RerankResultDto(candidates.get(3).chunkId().toString(), 0.610, 5)
        );
        RerankResponse mockRerankResponse = new RerankResponse(mockResults);

        stubFor(post(urlEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockRerankResponse))));

        List<RerankedChunk> reranked = orchestrator.rerankChunks(query, candidates);

        // Assert: returned in order, scores correct
        assertThat(reranked).hasSize(5);
        assertThat(reranked.get(0).chunkId()).isEqualTo(candidates.get(2).chunkId());
        assertThat(reranked.get(0).score()).isEqualTo(0.985);
        assertThat(reranked.get(0).rank()).isEqualTo(1);

        assertThat(reranked.get(1).chunkId()).isEqualTo(candidates.get(0).chunkId());
        assertThat(reranked.get(1).score()).isEqualTo(0.920);

        assertThat(reranked.get(4).chunkId()).isEqualTo(candidates.get(3).chunkId());
        assertThat(reranked.get(4).score()).isEqualTo(0.610);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 2: Reranker service timeout: request delayed past timeout falls back to RRF ranking")
    void testRerankerServiceTimeoutFallsBackToRrf() {
        List<ScoredChunk> candidates = createCandidateChunks(5);
        String query = "what does borderline LDL mean";

        // Mock: 5s delay on request (exceeds client's timeout)
        stubFor(post(urlEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000)
                        .withBody("{\"results\":[]}")));

        // Configure client with 1s timeout and 0 retries to verify timeout handling within 5s test limit
        RerankingClient timeoutClient = new RerankingClient(
                "http://localhost:" + wireMockServer.port() + "/rerank",
                circuitBreaker,
                objectMapper,
                httpRequest -> java.net.http.HttpClient.newHttpClient().send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString()),
                1, 0, 0L
        );
        RerankingOrchestrator timeoutOrchestrator = new RerankingOrchestrator(timeoutClient);

        // Directly verifying client throws RerankerUnavailableException on timeout
        assertThatThrownBy(() -> {
            timeoutClient.rerank(new com.medisense.rag.reranking.model.RerankRequest(query, List.of(), 5));
        }).isInstanceOf(RerankerUnavailableException.class);

        // Reset circuit breaker so orchestrator calls client and applies RRF fallback
        circuitBreaker.reset();

        // Assert: orchestrator falls back to RRF ranking
        List<RerankedChunk> fallbackResults = timeoutOrchestrator.rerankChunks(query, candidates);

        assertThat(fallbackResults).hasSize(5);
        // Candidate 0 was top in RRF candidates, so it should be rank 1 in fallback
        assertThat(fallbackResults.get(0).chunkId()).isEqualTo(candidates.get(0).chunkId());
        assertThat(fallbackResults.get(0).rank()).isEqualTo(1);
        assertThat(fallbackResults.get(0).isReranked()).isFalse();
    }

    @Test
    @DisplayName("Case 3: Circuit breaker: 3 consecutive failures opens circuit; 4th call fast-fails without HTTP request")
    void testCircuitBreakerTripsAfter3ConsecutiveFailures() {
        stubFor(post(urlEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error in Cross-Encoder")));

        com.medisense.rag.reranking.model.RerankRequest dummyRequest =
                new com.medisense.rag.reranking.model.RerankRequest("test query", List.of(), 5);

        // Calls 1 to 3 fail against the microservice
        for (int i = 1; i <= 3; i++) {
            assertThatThrownBy(() -> rerankerClient.rerank(dummyRequest))
                    .isInstanceOf(RerankerUnavailableException.class);
        }

        // Assert: circuit open after 3rd failure
        assertThat(circuitBreaker.getState()).isEqualTo(RerankerCircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.allowExecution()).isFalse();

        int requestsBeforeCall4 = wireMockServer.getAllServeEvents().size();

        // Call 4 fails immediately without calling service (fallback used)
        assertThatThrownBy(() -> rerankerClient.rerank(dummyRequest))
                .isInstanceOf(RerankerUnavailableException.class)
                .hasMessageContaining("Circuit breaker is OPEN");

        int requestsAfterCall4 = wireMockServer.getAllServeEvents().size();
        assertThat(requestsAfterCall4)
                .as("No HTTP request should be dispatched when circuit breaker is OPEN")
                .isEqualTo(requestsBeforeCall4);
    }

    @Test
    @DisplayName("Case 4: Circuit breaker recovery: after 60s cooldown, circuit closes on successful call")
    void testCircuitBreakerRecoveryAfter60s() throws Exception {
        // Step 1: Trip circuit breaker with 3 failures
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordFailure();
        }
        assertThat(circuitBreaker.getState()).isEqualTo(RerankerCircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.allowExecution()).isFalse();

        // Step 2: Advance time by 60s (RESET_TIMEOUT_MS)
        testClock.advanceSeconds(61);

        // Service has recovered
        UUID chunkId = UUID.randomUUID();
        RerankResponse mockRecoveryResponse = new RerankResponse(List.of(
                new RerankResultDto(chunkId.toString(), 0.95, 1)
        ));
        stubFor(post(urlEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockRecoveryResponse))));

        // Circuit breaker allows trial execution
        assertThat(circuitBreaker.allowExecution()).isTrue();

        com.medisense.rag.reranking.model.RerankRequest request =
                new com.medisense.rag.reranking.model.RerankRequest("recovered query", List.of(), 1);

        RerankResponse response = rerankerClient.rerank(request);

        // Assert: circuit closes, normal routing resumes
        assertThat(response).isNotNull();
        assertThat(circuitBreaker.getState()).isEqualTo(RerankerCircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getConsecutiveFailures()).isEqualTo(0);
    }
}
