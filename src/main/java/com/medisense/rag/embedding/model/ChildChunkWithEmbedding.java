package com.medisense.rag.embedding.model;

import com.medisense.rag.chunking.model.ChildChunk;

public record ChildChunkWithEmbedding(
        ChildChunk childChunk,
        float[] embedding
) {}
