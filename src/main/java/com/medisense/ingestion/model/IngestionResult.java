package com.medisense.ingestion.model;

import java.util.ArrayList;
import java.util.List;

public class IngestionResult {
    private String documentName;
    private List<StructuredNode> safeNodes = new ArrayList<>();
    private List<String> excludedNodesLog = new ArrayList<>();
    private int totalNodesScanned;
    private int excludedNodeCount;

    public IngestionResult() {}

    public IngestionResult(String documentName, List<StructuredNode> safeNodes, List<String> excludedNodesLog,
                           int totalNodesScanned, int excludedNodeCount) {
        this.documentName = documentName;
        this.safeNodes = safeNodes != null ? safeNodes : new ArrayList<>();
        this.excludedNodesLog = excludedNodesLog != null ? excludedNodesLog : new ArrayList<>();
        this.totalNodesScanned = totalNodesScanned;
        this.excludedNodeCount = excludedNodeCount;
    }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public List<StructuredNode> getSafeNodes() { return safeNodes; }
    public void setSafeNodes(List<StructuredNode> safeNodes) { this.safeNodes = safeNodes; }

    public List<String> getExcludedNodesLog() { return excludedNodesLog; }
    public void setExcludedNodesLog(List<String> excludedNodesLog) { this.excludedNodesLog = excludedNodesLog; }

    public int getTotalNodesScanned() { return totalNodesScanned; }
    public void setTotalNodesScanned(int totalNodesScanned) { this.totalNodesScanned = totalNodesScanned; }

    public int getExcludedNodeCount() { return excludedNodeCount; }
    public void setExcludedNodeCount(int excludedNodeCount) { this.excludedNodeCount = excludedNodeCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String documentName;
        private List<StructuredNode> safeNodes = new ArrayList<>();
        private List<String> excludedNodesLog = new ArrayList<>();
        private int totalNodesScanned;
        private int excludedNodeCount;

        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public Builder safeNodes(List<StructuredNode> safeNodes) { this.safeNodes = safeNodes; return this; }
        public Builder excludedNodesLog(List<String> excludedNodesLog) { this.excludedNodesLog = excludedNodesLog; return this; }
        public Builder totalNodesScanned(int totalNodesScanned) { this.totalNodesScanned = totalNodesScanned; return this; }
        public Builder excludedNodeCount(int excludedNodeCount) { this.excludedNodeCount = excludedNodeCount; return this; }

        public IngestionResult build() {
            return new IngestionResult(documentName, safeNodes, excludedNodesLog, totalNodesScanned, excludedNodeCount);
        }
    }
}
