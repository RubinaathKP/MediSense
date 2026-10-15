package com.medisense.rag.chunking.model;

import java.util.ArrayList;
import java.util.List;

public class ChunkPair {
    private ParentChunk parentChunk;
    private List<ChildChunk> childChunks = new ArrayList<>();

    public ChunkPair() {}

    public ChunkPair(ParentChunk parentChunk, List<ChildChunk> childChunks) {
        this.parentChunk = parentChunk;
        this.childChunks = childChunks != null ? childChunks : new ArrayList<>();
    }

    public ParentChunk getParentChunk() { return parentChunk; }
    public ParentChunk parentChunk() { return parentChunk; }
    public void setParentChunk(ParentChunk parentChunk) { this.parentChunk = parentChunk; }

    public List<ChildChunk> getChildChunks() { return childChunks; }
    public List<ChildChunk> childChunks() { return childChunks; }
    public void setChildChunks(List<ChildChunk> childChunks) { this.childChunks = childChunks; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ParentChunk parentChunk;
        private List<ChildChunk> childChunks = new ArrayList<>();

        public Builder parentChunk(ParentChunk parentChunk) { this.parentChunk = parentChunk; return this; }
        public Builder childChunks(List<ChildChunk> childChunks) { this.childChunks = childChunks; return this; }

        public ChunkPair build() {
            return new ChunkPair(parentChunk, childChunks);
        }
    }
}
