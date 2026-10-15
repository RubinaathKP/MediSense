package com.medisense.api.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
public class RateLimitConfig {

    public record RateLimitRule(
            String endpointSlug,
            long capacity,
            Duration duration,
            boolean byIp,
            boolean byAdminKey
    ) {}

    private final Map<String, RateLimitRule> rules = new HashMap<>();

    public RateLimitConfig() {
        // TIER 1 - Auth Endpoints
        registerRule("POST:/auth/register", 5, Duration.ofHours(1), true, false);
        registerRule("POST:/api/v1/auth/register", 5, Duration.ofHours(1), true, false);

        registerRule("POST:/auth/login", 10, Duration.ofHours(1), true, false);
        registerRule("POST:/api/v1/auth/login", 10, Duration.ofHours(1), true, false);

        registerRule("POST:/auth/refresh", 30, Duration.ofHours(1), false, false);
        registerRule("POST:/api/v1/auth/refresh", 30, Duration.ofHours(1), false, false);

        // TIER 2 - Report Ingestion
        registerRule("POST:/reports/upload", 10, Duration.ofHours(1), false, false);
        registerRule("POST:/api/v1/reports/upload", 10, Duration.ofHours(1), false, false);

        registerRule("POST:/reports/abdm-pull", 5, Duration.ofHours(1), false, false);
        registerRule("POST:/api/v1/reports/abdm-pull", 5, Duration.ofHours(1), false, false);

        registerRule("POST:/reports/confirm-value", 30, Duration.ofHours(1), false, false);
        registerRule("POST:/api/v1/reports/confirm-value", 30, Duration.ofHours(1), false, false);

        // TIER 3 - Query Endpoints
        registerRule("POST:/query", 30, Duration.ofHours(1), false, false);
        registerRule("POST:/api/v1/query", 30, Duration.ofHours(1), false, false);

        registerRule("POST:/claims/verify", 20, Duration.ofHours(1), false, false);
        registerRule("POST:/api/v1/claims/verify", 20, Duration.ofHours(1), false, false);

        // TIER 4 - Read Endpoints
        registerRule("GET:/query/", 100, Duration.ofHours(1), false, false); // Prefix match for /query/{id}
        registerRule("GET:/api/v1/query/", 100, Duration.ofHours(1), false, false);

        registerRule("GET:/session/", 60, Duration.ofHours(1), false, false); // Prefix match for /session/{id}/history
        registerRule("GET:/api/v1/session/", 60, Duration.ofHours(1), false, false);
        registerRule("GET:/reports/", 60, Duration.ofHours(1), false, false); // /reports/{session_id}
        registerRule("GET:/api/v1/reports/", 60, Duration.ofHours(1), false, false);

        // TIER 5 - Internal / Admin
        registerRule("POST:/internal/eval/run", 3, Duration.ofHours(1), false, true);
        registerRule("POST:/api/v1/internal/eval/run", 3, Duration.ofHours(1), false, true);

        registerRule("GET:/internal/eval/results/", 60, Duration.ofHours(1), false, true);
        registerRule("GET:/api/v1/internal/eval/results/", 60, Duration.ofHours(1), false, true);

        registerRule("GET:/internal/audit/events", 10, Duration.ofHours(1), false, true);
        registerRule("GET:/api/v1/internal/audit/events", 10, Duration.ofHours(1), false, true);

        registerRule("GET:/internal/audit/security-events", 60, Duration.ofHours(1), false, true);
        registerRule("GET:/api/v1/internal/audit/security-events", 60, Duration.ofHours(1), false, true);
    }

    private void registerRule(String key, long capacity, Duration duration, boolean byIp, boolean byAdminKey) {
        rules.put(key, new RateLimitRule(key, capacity, duration, byIp, byAdminKey));
    }

    public Optional<RateLimitRule> findRule(String method, String uri) {
        String exactKey = method + ":" + uri;
        if (rules.containsKey(exactKey)) {
            return Optional.of(rules.get(exactKey));
        }

        // Prefix match for path variables like /query/{id} or /internal/eval/results/*
        for (Map.Entry<String, RateLimitRule> entry : rules.entrySet()) {
            String ruleKey = entry.getKey();
            if (ruleKey.endsWith("/") && exactKey.startsWith(ruleKey)) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    public Bucket createQueryBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillGreedy(30, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
