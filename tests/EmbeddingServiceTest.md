# Test Specification: EmbeddingServiceTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.rag.embedding.EmbeddingService`
- **Category:** Unit Test (Vector Store & Resilience)
- **Framework:** JUnit 5, Mockito, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/rag/embedding/EmbeddingServiceUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.rag.embedding;

import com.medisense.UnitTestBase;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.embedding.FallbackEmbeddingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class EmbeddingServiceUnitTest extends UnitTestBase {

    @Mock
    private FallbackEmbeddingClient fallbackClient;

    private EmbeddingService embeddingService;

    @Override
    protected void onSetup() {
        embeddingService = new EmbeddingService(fallbackClient, null);
    }

    @Test
    @DisplayName("Should generate deterministic 64-char SHA256 hex string for query caching")
    void shouldGenerateSha256ForQuery() {
        String query = "What is normal LDL cholesterol?";
        String hash = embeddingService.computeSha256(query.trim().toLowerCase());

        assertThat(hash).isNotNull().hasSize(64);
        assertThat(hash).isEqualTo(embeddingService.computeSha256("what is normal ldl cholesterol?"));
    }

    @Test
    @DisplayName("Should correctly serialize float array to PostgreSQL vector string format and parse back")
    void shouldSerializeAndParseVectors() {
        float[] vector = new float[768];
        vector[0] = 0.85f;
        vector[100] = -0.42f;
        vector[767] = 0.12f;

        String vectorStr = embeddingService.vectorToString(vector);
        assertThat(vectorStr).startsWith("[").endsWith("]");

        float[] parsed = embeddingService.parseVectorString(vectorStr);
        assertThat(parsed).hasSize(768);
        assertThat(parsed[0]).isEqualTo(0.85f);
        assertThat(parsed[100]).isEqualTo(-0.42f);
        assertThat(parsed[767]).isEqualTo(0.12f);
    }

    @Test
    @DisplayName("Should trip circuit breaker and use fallback embedding client on failure threshold")
    void shouldFallbackWhenPrimaryFails() {
        when(fallbackClient.generateFallbackEmbedding(anyString()))
                .thenReturn(new float[768]);

        for (int i = 0; i < 6; i++) {
            embeddingService.recordFailure();
        }

        assertThat(embeddingService.isCircuitBreakerOpen()).isTrue();

        float[] embedding = embeddingService.generateQueryEmbedding("Clinical query text");
        assertThat(embedding).hasSize(768);
    }
}
```
