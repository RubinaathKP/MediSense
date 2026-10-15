package com.medisense.rag.retrieval.model;

import java.util.UUID;

public record ScoredChunk(
        UUID chunkId,
        String sectionHeading,
        String content,
        double score,
        int rank,
        String source
) {
    public ScoredChunk(String id, String content, double score, String source) {
        this(parseUuidSafe(id), "Section", content, score, 0, source);
    }

    private static UUID parseUuidSafe(String id) {
        try {
            return UUID.fromString(id);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
