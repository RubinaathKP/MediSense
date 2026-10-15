package com.medisense.config.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * ABDMGatewayHealthIndicator — Actuator health check for ABDM Sandbox Gateway.
 */
@Component
public class ABDMGatewayHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ABDMGatewayHealthIndicator.class);

    private final String gatewayUrl;
    private final String clientId;
    private final RestClient restClient;

    public ABDMGatewayHealthIndicator(
            @Value("${medisense.abdm.gateway-url:https://dev.abdm.gov.in/gateway}") String gatewayUrl,
            @Value("${medisense.abdm.client-id:}") String clientId
    ) {
        this.gatewayUrl = gatewayUrl;
        this.clientId = clientId;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public Health health() {
        if (clientId == null || clientId.isBlank() || clientId.contains("PLACEHOLDER")) {
            return Health.up()
                    .withDetail("gateway_url", gatewayUrl)
                    .withDetail("status", "SANDBOX_MOCK_READY")
                    .withDetail("message", "ABDM Gateway configured in sandbox development mode")
                    .build();
        }

        try {
            long start = System.currentTimeMillis();
            // Ping ABDM gateway endpoint or public health endpoint
            var response = restClient.get()
                    .uri(gatewayUrl + "/v0.5/users/auth/fetch-modes")
                    .retrieve()
                    .toBodilessEntity();

            long latencyMs = System.currentTimeMillis() - start;
            return Health.up()
                    .withDetail("gateway_url", gatewayUrl)
                    .withDetail("latency_ms", latencyMs)
                    .withDetail("status_code", response.getStatusCode().value())
                    .build();
        } catch (Exception e) {
            log.debug("ABDM gateway ping failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("gateway_url", gatewayUrl)
                    .withDetail("error", e.getMessage())
                    .withDetail("fallback", "PDF_UPLOAD_AVAILABLE")
                    .build();
        }
    }
}
