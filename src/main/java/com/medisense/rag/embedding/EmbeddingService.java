package com.medisense.rag.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_DELAY_MS = 30000L;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Value("${medisense.llm.model-name:text-multilingual-embedding-002}")
    private String embeddingModelName;

    private final FallbackEmbeddingClient fallbackClient;
    private final StringRedisTemplate redisTemplate;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile boolean circuitBreakerOpen = false;

    public EmbeddingService() {
        this.fallbackClient = new DefaultFallbackEmbeddingClient();
        this.redisTemplate = null;
    }

    @Autowired
    public EmbeddingService(FallbackEmbeddingClient fallbackClient,
                            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.fallbackClient = fallbackClient != null ? fallbackClient : new DefaultFallbackEmbeddingClient();
        this.redisTemplate = redisTemplate;
    }

    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[768];
        }

        String normalized = text.trim().toLowerCase();
        String cacheKey = "embedding:query:" + computeSha256(normalized);

        // 1. Check Redis Cache for Query Embedding
        if (redisTemplate != null) {
            try {
                String cachedVector = redisTemplate.opsForValue().get(cacheKey);
                if (cachedVector != null && !cachedVector.isBlank()) {
                    log.info("Redis cache HIT for query embedding SHA256 key: {}", cacheKey);
                    return parseVectorString(cachedVector);
                }
                log.debug("Redis cache MISS for query embedding key: {}", cacheKey);
            } catch (Exception e) {
                log.warn("Redis query embedding lookup failed: {}", e.getMessage());
            }
        }

        // 2. Generate Embedding via Primary or Fallback
        float[] embedding = generateSingleEmbeddingWithRetry(text);

        // 3. Cache generated embedding in Redis (24h TTL)
        if (redisTemplate != null && embedding != null && embedding.length == 768) {
            try {
                redisTemplate.opsForValue().set(cacheKey, vectorToString(embedding), CACHE_TTL);
                log.debug("Cached query embedding in Redis with TTL 24h.");
            } catch (Exception e) {
                log.warn("Failed to write query embedding to Redis cache: {}", e.getMessage());
            }
        }

        return embedding;
    }

    public List<float[]> generateEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        log.info("Generating batch embeddings for {} items (Max Sub-batch: {}). Model: '{}'",
                texts.size(), MAX_BATCH_SIZE, embeddingModelName);

        List<float[]> results = new ArrayList<>(texts.size());

        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> subBatch = texts.subList(i, end);

            if (circuitBreakerOpen) {
                log.warn("Circuit Breaker is OPEN. Routing batch sub-list [{}-{}] directly to Fallback Client.", i, end - 1);
                results.addAll(fallbackClient.generateFallbackEmbeddingsBatch(subBatch));
            } else {
                for (String text : subBatch) {
                    results.add(generateSingleEmbeddingWithRetry(text));
                }
            }
        }

        return results;
    }

    private float[] generateSingleEmbeddingWithRetry(String text) {
        if (circuitBreakerOpen) {
            log.warn("Circuit Breaker is OPEN. Using FallbackEmbeddingClient.");
            return fallbackClient.generateFallbackEmbedding(text);
        }

        int attempt = 0;
        long currentDelay = INITIAL_DELAY_MS;

        while (attempt <= MAX_RETRIES) {
            try {
                attempt++;
                float[] vector = invokeGoogleEmbeddingApi(text);
                resetCircuitBreaker();
                return vector;
            } catch (Exception e) {
                log.warn("Google Multilingual Embedding API call failed (Attempt {}/{}): {}",
                        attempt, MAX_RETRIES + 1, e.getMessage());

                recordFailure();

                if (circuitBreakerOpen) {
                    log.error("Circuit Breaker tripped to OPEN due to {} consecutive failures. Switching to Fallback Client.",
                            CIRCUIT_BREAKER_THRESHOLD);
                    return fallbackClient.generateFallbackEmbedding(text);
                }

                if (attempt > MAX_RETRIES) {
                    log.error("Max retries reached for Google Embedding API. Using Fallback Client.");
                    return fallbackClient.generateFallbackEmbedding(text);
                }

                try {
                    long delayWithJitter = currentDelay + (long) (Math.random() * 200);
                    Thread.sleep(Math.min(delayWithJitter, MAX_DELAY_MS));
                    currentDelay = (long) Math.min(currentDelay * BACKOFF_MULTIPLIER, MAX_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during exponential backoff", ie);
                }
            }
        }

        return fallbackClient.generateFallbackEmbedding(text);
    }

    private float[] invokeGoogleEmbeddingApi(String text) {
        // Simulates Google text-multilingual-embedding-002 SDK call
        log.trace("Invoking Google Generative AI Java SDK (model: {}) for snippet length {}",
                embeddingModelName, text != null ? text.length() : 0);

        float[] vector = new float[768];
        for (int i = 0; i < 768; i++) {
            vector[i] = (float) (Math.cos(i + (text != null ? text.hashCode() : 0)) * 0.5);
        }
        return vector;
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpen = true;
            log.error("CIRCUIT BREAKER TRIPPED: {} consecutive embedding API failures.", failures);
        }
    }

    public float[] generateQueryEmbedding(String text) {
        return generateEmbedding(text);
    }

    public void resetCircuitBreaker() {
        consecutiveFailures.set(0);
        if (circuitBreakerOpen) {
            circuitBreakerOpen = false;
            log.info("Circuit Breaker reset to CLOSED. Primary embedding API recovered.");
        }
    }

    public boolean isCircuitBreakerOpen() {
        return circuitBreakerOpen;
    }

    public void setCircuitBreakerOpen(boolean open) {
        this.circuitBreakerOpen = open;
    }

    public String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }

    public float[] parseVectorString(String vectorStr) {
        if (vectorStr == null || !vectorStr.startsWith("[") || !vectorStr.endsWith("]")) {
            return new float[768];
        }
        String content = vectorStr.substring(1, vectorStr.length() - 1);
        if (content.isBlank()) return new float[768];

        String[] tokens = content.split(",");
        float[] vector = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            vector[i] = Float.parseFloat(tokens[i].trim());
        }
        return vector;
    }

    public String vectorToString(float[] vector) {
        if (vector == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
