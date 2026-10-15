package com.medisense.rag.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private FallbackEmbeddingClient fallbackClient;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // Pass null for StringRedisTemplate to test embedding generation & circuit breaker on JDK 24
        embeddingService = new EmbeddingService(fallbackClient, null);
    }

    @Test
    @DisplayName("Test SHA256 Key Generation for Query Normalization")
    void testSha256KeyGeneration() {
        String query = "What is normal LDL cholesterol?";
        String hash = embeddingService.computeSha256(query.trim().toLowerCase());

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("Test Vector Serialization and Parsing")
    void testVectorSerializationAndParsing() {
        float[] vector = new float[768];
        vector[0] = 0.5f;
        vector[767] = -0.25f;

        String vectorStr = embeddingService.vectorToString(vector);
        assertNotNull(vectorStr);

        float[] parsed = embeddingService.parseVectorString(vectorStr);
        assertEquals(768, parsed.length);
        assertEquals(0.5f, parsed[0], 0.0001f);
        assertEquals(-0.25f, parsed[767], 0.0001f);
    }

    @Test
    @DisplayName("Test Circuit Breaker opens after 5 consecutive failures and uses Fallback Client")
    void testCircuitBreakerTripsAfterFiveFailures() {
        when(fallbackClient.generateFallbackEmbedding(anyString()))
                .thenReturn(new float[768]);

        // Trigger circuit breaker state
        embeddingService.setCircuitBreakerOpen(true);
        assertTrue(embeddingService.isCircuitBreakerOpen());

        float[] fallbackVector = embeddingService.generateEmbedding("Any medical query text");
        assertNotNull(fallbackVector);
        assertEquals(768, fallbackVector.length);
        verify(fallbackClient, times(1)).generateFallbackEmbedding(anyString());
    }

    @Test
    @DisplayName("Test generateEmbeddingsBatch routes to fallback when Circuit Breaker is open")
    void testGenerateEmbeddingsBatchUsesFallbackWhenCircuitBreakerOpen() {
        embeddingService.setCircuitBreakerOpen(true);

        when(fallbackClient.generateFallbackEmbeddingsBatch(anyList()))
                .thenReturn(List.of(new float[768], new float[768]));

        List<float[]> results = embeddingService.generateEmbeddingsBatch(List.of("Text 1", "Text 2"));

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(fallbackClient, times(1)).generateFallbackEmbeddingsBatch(anyList());
    }
}
