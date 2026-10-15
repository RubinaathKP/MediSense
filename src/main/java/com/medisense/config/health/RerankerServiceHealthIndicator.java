package com.medisense.config.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * RerankerServiceHealthIndicator — Actuator health check for local Cross-Encoder Reranker microservice.
 * Reports UP / DOWN / FALLBACK_ACTIVE (RRF ranker).
 */
@Component
public class RerankerServiceHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RerankerServiceHealthIndicator.class);

    private final String rerankerUrl;
    private final RestClient restClient;

    public RerankerServiceHealthIndicator(
            @Value("${medisense.reranker.url:http://localhost:8001}") String rerankerUrl
    ) {
        this.rerankerUrl = rerankerUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public Health health() {
        try {
            long start = System.currentTimeMillis();
            var response = restClient.get()
                    .uri(rerankerUrl + "/health")
                    .retrieve()
                    .toBodilessEntity();

            long latencyMs = System.currentTimeMillis() - start;
            return Health.up()
                    .withDetail("service", "Cross-Encoder Reranker")
                    .withDetail("url", rerankerUrl)
                    .withDetail("latency_ms", latencyMs)
                    .withDetail("status_code", response.getStatusCode().value())
                    .build();
        } catch (Exception e) {
            log.debug("Reranker service health check failed, RRF fallback active: {}", e.getMessage());
            return Health.status("FALLBACK_ACTIVE")
                    .withDetail("service", "Cross-Encoder Reranker")
                    .withDetail("url", rerankerUrl)
                    .withDetail("error", e.getMessage())
                    .withDetail("fallback", "Reciprocal Rank Fusion (RRF) Active")
                    .build();
        }
    }
}
