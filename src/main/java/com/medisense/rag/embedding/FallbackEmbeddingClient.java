package com.medisense.rag.embedding;

import java.util.List;

public interface FallbackEmbeddingClient {
    float[] generateFallbackEmbedding(String text);
    List<float[]> generateFallbackEmbeddingsBatch(List<String> texts);
}
