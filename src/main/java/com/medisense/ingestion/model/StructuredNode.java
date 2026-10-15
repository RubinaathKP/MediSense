package com.medisense.ingestion.model;

import java.util.ArrayList;
import java.util.List;

public class StructuredNode {
    private NodeLevel level;
    private String heading;
    private String textContent;
    private String positionPath;
    private List<StructuredNode> children = new ArrayList<>();

    public StructuredNode() {}

    public StructuredNode(NodeLevel level, String heading, String textContent, String positionPath, List<StructuredNode> children) {
        this.level = level;
        this.heading = heading;
        this.textContent = textContent;
        this.positionPath = positionPath;
        this.children = children != null ? children : new ArrayList<>();
    }

    public NodeLevel getLevel() { return level; }
    public void setLevel(NodeLevel level) { this.level = level; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getPositionPath() { return positionPath; }
    public void setPositionPath(String positionPath) { this.positionPath = positionPath; }

    public List<StructuredNode> getChildren() { return children; }
    public void setChildren(List<StructuredNode> children) { this.children = children; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private NodeLevel level;
        private String heading;
        private String textContent;
        private String positionPath;
        private List<StructuredNode> children = new ArrayList<>();

        public Builder level(NodeLevel level) { this.level = level; return this; }
        public Builder heading(String heading) { this.heading = heading; return this; }
        public Builder textContent(String textContent) { this.textContent = textContent; return this; }
        public Builder positionPath(String positionPath) { this.positionPath = positionPath; return this; }
        public Builder children(List<StructuredNode> children) { this.children = children; return this; }

        public StructuredNode build() {
            return new StructuredNode(level, heading, textContent, positionPath, children);
        }
    }
}
