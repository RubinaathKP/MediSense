package com.medisense.translation.indictrans2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.translation.indictrans2.model.IndicTrans2Request;
import com.medisense.translation.indictrans2.model.IndicTrans2Response;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
public class IndicTrans2Client {

    private static final Logger log = LoggerFactory.getLogger(IndicTrans2Client.class);

    private static final int TIMEOUT_SECONDS = 3;
    private static final int MAX_RETRIES = 2; // 2 retries = 3 total attempts
    private static final long RETRY_DELAY_MS = 500L;
    private static final int RATE_LIMIT_CAPACITY = 100;

    @FunctionalInterface
    public interface HttpSender {
        HttpResponse<String> send(HttpRequest request) throws Exception;
    }

    private final String serviceUrl;
    private final IndicTrans2CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;
    private final HttpSender httpSender;
    private final Bucket rateLimiterBucket;

    public IndicTrans2Client(@Value("${medisense.translation.indictrans2.url:http://indictrans2-service:8002/translate}") String serviceUrl,
                             IndicTrans2CircuitBreaker circuitBreaker,
                             ObjectMapper objectMapper) {
        this(serviceUrl, circuitBreaker, objectMapper, defaultHttpSender());
    }

    public IndicTrans2Client(String serviceUrl,
                             IndicTrans2CircuitBreaker circuitBreaker,
                             ObjectMapper objectMapper,
                             HttpSender httpSender) {
        this.serviceUrl = serviceUrl;
        this.circuitBreaker = circuitBreaker;
        this.objectMapper = objectMapper;
        this.httpSender = httpSender;

        Bandwidth limit = Bandwidth.builder()
                .capacity(RATE_LIMIT_CAPACITY)
                .refillIntervally(RATE_LIMIT_CAPACITY, Duration.ofMinutes(1))
                .build();
        this.rateLimiterBucket = Bucket.builder().addLimit(limit).build();
    }

    private static HttpSender defaultHttpSender() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        return req -> client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public IndicTrans2Response translate(String maskedText, String srcLang, String tgtLang) {
        if (!circuitBreaker.allowExecution()) {
            throw new IndicTrans2UnavailableException("Circuit breaker is OPEN for AI4Bharat IndicTrans2 service");
        }

        // 1. Bucket4j Rate Limiting
        try {
            boolean acquired = rateLimiterBucket.asBlocking().tryConsume(1, Duration.ofSeconds(3));
            if (!acquired) {
                throw new IndicTrans2UnavailableException("IndicTrans2 Bucket4j rate limit wait exceeded 3 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IndicTrans2UnavailableException("Rate limit wait interrupted", e);
        }

        IndicTrans2Request request = new IndicTrans2Request(maskedText, srcLang, tgtLang);
        Exception lastException = null;
        int totalAttempts = 1 + MAX_RETRIES;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                log.info("Calling AI4Bharat IndicTrans2 Python microservice (attempt {}/{}): url={}", attempt, totalAttempts, serviceUrl);
                String requestJson = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(serviceUrl))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = httpSender.send(httpRequest);

                if (response.statusCode() == 200) {
                    IndicTrans2Response indicResponse = objectMapper.readValue(response.body(), IndicTrans2Response.class);
                    circuitBreaker.recordSuccess();
                    log.info("AI4Bharat IndicTrans2 translation succeeded cleanly.");
                    return indicResponse;
                } else {
                    throw new RuntimeException("IndicTrans2 HTTP failure: status=" + response.statusCode() + ", body=" + response.body());
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("IndicTrans2 request attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < totalAttempts) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IndicTrans2UnavailableException("Interrupted during retry delay", ie);
                    }
                }
            }
        }

        circuitBreaker.recordFailure();
        throw new IndicTrans2UnavailableException("AI4Bharat IndicTrans2 microservice failed after " + totalAttempts + " attempts", lastException);
    }
}
