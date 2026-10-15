package com.medisense.rag.retrieval.model;

import java.util.List;
import java.util.UUID;

public record RetrievedContext(
        String parentChunkText,
        String sourceName,
        int sourceYear,
        String evidenceBasis,
        String sectionHeading,
        List<UUID> triggeredChildChunkIds
) {
    public RetrievedContext(UUID id, String parentChunkText, double score, String sourceName, int sourceYear, String evidenceBasis, List<UUID> triggeredChildChunkIds) {
        this(parentChunkText, sourceName, sourceYear, evidenceBasis, "Section", triggeredChildChunkIds);
    }
}
