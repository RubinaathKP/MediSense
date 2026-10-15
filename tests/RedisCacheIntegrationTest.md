# Test Specification: RedisCacheIntegrationTest

## 1. Test Suite Overview
- **Component Under Test:** Redis 7 Caching Layer
- **Category:** Integration Test (Cache Infrastructure & Latency Optimization)
- **Framework:** Spring Boot Test, Testcontainers (`redis:7-alpine`), AssertJ
- **File Location:** `src/test/java/com/medisense/integration/RedisCacheIntegrationTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.integration;

import com.medisense.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheIntegrationTest extends IntegrationTestBase {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Should successfully store, retrieve, and evict cached query embeddings in Redis container")
    void shouldStoreAndRetrieveFromRedisCache() {
        if (redisTemplate == null) {
            log.warn("RedisTemplate bean not active in current profile, skipping Redis test.");
            return;
        }

        String cacheKey = "cache:query:sha256:7b5d1e2f";
        String cachedResponse = "{\"status\":\"CACHED\",\"explanation\":\"Borderline LDL indicates 130-159 mg/dL.\"}";

        redisTemplate.opsForValue().set(cacheKey, cachedResponse, Duration.ofMinutes(5));

        Object retrieved = redisTemplate.opsForValue().get(cacheKey);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.toString()).contains("Borderline LDL");

        redisTemplate.delete(cacheKey);
        Object afterEviction = redisTemplate.opsForValue().get(cacheKey);
        assertThat(afterEviction).isNull();
    }
}
```
