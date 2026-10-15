package com.medisense.rag.retrieval.model;

public record HyDEExpansionResult(
        String originalQuery,
        float[] originalQueryEmbedding,
        String hypotheticalAnswer,
        float[] hydeEmbedding
) {
    public String getOriginalQuery() {
        return originalQuery;
    }

    public float[] getOriginalQueryEmbedding() {
        return originalQueryEmbedding;
    }

    public String getHypotheticalAnswer() {
        return hypotheticalAnswer;
    }

    public float[] getHydeEmbedding() {
        return hydeEmbedding;
    }
}
