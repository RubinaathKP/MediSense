package com.medisense.rag.reranking.model;

import java.util.UUID;

public record RerankedChunk(
        UUID chunkId,
        String sectionHeading,
        String content,
        double score,
        int rank,
        String source,
        boolean isReranked
) {}
