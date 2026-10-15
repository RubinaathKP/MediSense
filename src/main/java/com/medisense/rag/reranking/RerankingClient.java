package com.medisense.rag.reranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.reranking.exception.RerankerUnavailableException;
import com.medisense.rag.reranking.model.RerankRequest;
import com.medisense.rag.reranking.model.RerankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class RerankingClient implements RerankerClient {

    private static final Logger log = LoggerFactory.getLogger(RerankingClient.class);

    private static final int TIMEOUT_SECONDS = 3;
    private static final int MAX_RETRIES = 2; // 2 retries = 3 total attempts
    private static final long RETRY_DELAY_MS = 500L;

    @FunctionalInterface
    public interface HttpSender {
        HttpResponse<String> send(HttpRequest request) throws Exception;
    }

    private final String rerankerUrl;
    private final RerankerCircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;
    private final HttpSender httpSender;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final long retryDelayMs;

    public RerankingClient(@Value("${medisense.reranker.url:http://reranker-service:8001/rerank}") String rerankerUrl,
                           RerankerCircuitBreaker circuitBreaker,
                           ObjectMapper objectMapper) {
        this(rerankerUrl, circuitBreaker, objectMapper, defaultHttpSender());
    }

    public RerankingClient(String rerankerUrl,
                           RerankerCircuitBreaker circuitBreaker,
                           ObjectMapper objectMapper,
                           HttpSender httpSender) {
        this(rerankerUrl, circuitBreaker, objectMapper, httpSender, TIMEOUT_SECONDS, MAX_RETRIES, RETRY_DELAY_MS);
    }

    public RerankingClient(String rerankerUrl,
                           RerankerCircuitBreaker circuitBreaker,
                           ObjectMapper objectMapper,
                           HttpSender httpSender,
                           int timeoutSeconds,
                           int maxRetries,
                           long retryDelayMs) {
        this.rerankerUrl = rerankerUrl;
        this.circuitBreaker = circuitBreaker;
        this.objectMapper = objectMapper;
        this.httpSender = httpSender;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    private static HttpSender defaultHttpSender() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        return req -> client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public RerankResponse rerank(RerankRequest request) {
        if (!circuitBreaker.allowExecution()) {
            throw new RerankerUnavailableException("Circuit breaker is OPEN for reranker service");
        }

        Exception lastException = null;
        int totalAttempts = 1 + maxRetries;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                log.info("Calling Python reranker microservice (attempt {}/{}): url={}", attempt, totalAttempts, rerankerUrl);
                String requestJson = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(rerankerUrl))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = httpSender.send(httpRequest);

                if (response.statusCode() == 200) {
                    RerankResponse rerankResponse = objectMapper.readValue(response.body(), RerankResponse.class);
                    circuitBreaker.recordSuccess();
                    log.info("Reranker service call succeeded with {} results",
                            rerankResponse.results() != null ? rerankResponse.results().size() : 0);
                    return rerankResponse;
                } else {
                    throw new RuntimeException("Reranker HTTP failure: status=" + response.statusCode() + ", body=" + response.body());
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Reranker request attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < totalAttempts) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RerankerUnavailableException("Interrupted during retry delay", ie);
                    }
                }
            }
        }

        circuitBreaker.recordFailure();
        throw new RerankerUnavailableException("Reranker microservice failed after " + totalAttempts + " attempts", lastException);
    }
}
