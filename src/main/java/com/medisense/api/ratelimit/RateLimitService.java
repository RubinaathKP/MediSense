package com.medisense.api.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    public record ConsumptionResult(
            boolean isConsumed,
            long remainingTokens,
            long secondsToRefill,
            long capacity
    ) {}

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ConsumptionResult tryConsume(RateLimitConfig.RateLimitRule rule, String identifier) {
        long effectiveCapacity = rule.capacity();

        // Check if user or IP is flagged for abuse in Redis
        if (redisTemplate != null) {
            try {
                Boolean isUserFlagged = redisTemplate.hasKey("abuse:flag:" + identifier);
                Boolean isIpFlagged = redisTemplate.hasKey("abuse:ip:" + identifier);
                if (Boolean.TRUE.equals(isUserFlagged) || Boolean.TRUE.equals(isIpFlagged)) {
                    effectiveCapacity = Math.max(1, rule.capacity() / 2);
                    log.warn("Rate limiting flagged entity ({}): Tier capacity reduced by 50% to {}", identifier, effectiveCapacity);
                }
            } catch (Exception e) {
                log.debug("Redis abuse flag check bypassed: {}", e.getMessage());
            }
        }

        String key = "rate:" + sanitizeSlug(rule.endpointSlug()) + ":" + identifier;

        if (redisTemplate != null) {
            try {
                // Redis distributed rate limit check
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, rule.duration());
                }

                if (count != null && count <= effectiveCapacity) {
                    long remaining = effectiveCapacity - count;
                    Long ttlSeconds = redisTemplate.getExpire(key);
                    long refillSeconds = (ttlSeconds != null && ttlSeconds > 0) ? ttlSeconds : rule.duration().toSeconds();
                    return new ConsumptionResult(true, remaining, refillSeconds, effectiveCapacity);
                } else {
                    Long ttlSeconds = redisTemplate.getExpire(key);
                    long refillSeconds = (ttlSeconds != null && ttlSeconds > 0) ? ttlSeconds : rule.duration().toSeconds();
                    return new ConsumptionResult(false, 0, refillSeconds, effectiveCapacity);
                }
            } catch (Exception e) {
                log.debug("Redis rate limit bypass to local Bucket4j memory: {}", e.getMessage());
            }
        }

        // Fallback to in-memory Bucket4j instance
        Bucket bucket = localBuckets.computeIfAbsent(key, k -> createBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            long refillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            return new ConsumptionResult(true, probe.getRemainingTokens(), Math.max(1, refillSeconds), rule.capacity());
        } else {
            long refillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            return new ConsumptionResult(false, 0, Math.max(1, refillSeconds), rule.capacity());
        }
    }

    private Bucket createBucket(RateLimitConfig.RateLimitRule rule) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rule.capacity())
                .refillGreedy(rule.capacity(), rule.duration())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String sanitizeSlug(String slug) {
        return slug.replace(":", "_").replace("/", "_");
    }
}
