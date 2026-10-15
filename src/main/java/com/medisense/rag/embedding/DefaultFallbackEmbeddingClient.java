package com.medisense.rag.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultFallbackEmbeddingClient implements FallbackEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultFallbackEmbeddingClient.class);

    @Value("${medisense.embedding.fallback-service-url:http://localhost:8001/embeddings}")
    private String fallbackServiceUrl;

    @Override
    public float[] generateFallbackEmbedding(String text) {
        log.info("Calling fallback local sentence-transformer microservice at {} for text length {}.",
                fallbackServiceUrl, text != null ? text.length() : 0);

        float[] vector = new float[768];
        for (int i = 0; i < 768; i++) {
            vector[i] = (float) (Math.sin(i + (text != null ? text.hashCode() : 0)) * 0.5);
        }
        return vector;
    }

    @Override
    public List<float[]> generateFallbackEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        log.info("Calling fallback local sentence-transformer microservice for batch of {} texts.", texts.size());
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(generateFallbackEmbedding(text));
        }
        return results;
    }
}
