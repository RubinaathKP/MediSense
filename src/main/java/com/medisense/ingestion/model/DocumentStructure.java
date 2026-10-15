package com.medisense.ingestion.model;

import java.util.ArrayList;
import java.util.List;

public class DocumentStructure {
    private List<StructuredNode> rootNodes = new ArrayList<>();
    private int totalNodeCount;

    public DocumentStructure() {}

    public DocumentStructure(List<StructuredNode> rootNodes, int totalNodeCount) {
        this.rootNodes = rootNodes != null ? rootNodes : new ArrayList<>();
        this.totalNodeCount = totalNodeCount;
    }

    public List<StructuredNode> getRootNodes() { return rootNodes; }
    public void setRootNodes(List<StructuredNode> rootNodes) { this.rootNodes = rootNodes; }

    public int getTotalNodeCount() { return totalNodeCount; }
    public void setTotalNodeCount(int totalNodeCount) { this.totalNodeCount = totalNodeCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<StructuredNode> rootNodes = new ArrayList<>();
        private int totalNodeCount;

        public Builder rootNodes(List<StructuredNode> rootNodes) { this.rootNodes = rootNodes; return this; }
        public Builder totalNodeCount(int totalNodeCount) { this.totalNodeCount = totalNodeCount; return this; }

        public DocumentStructure build() {
            return new DocumentStructure(rootNodes, totalNodeCount);
        }
    }
}
