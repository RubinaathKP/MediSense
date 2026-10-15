package com.medisense.ingestion;

import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.ingestion.model.StructuredNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PrescriptiveContentDetectorService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptiveContentDetectorService.class);

    // 1. Dosage pattern + directive language (handles both "take 10mg" and "10mg daily")
    private static final Pattern DOSAGE_DIRECTIVE_PATTERN = Pattern.compile(
            "(?is).*(\\b\\d+\\s*(mg|ml|mcg|iu|units|g)\\b.*\\b(take|administer|prescribe|give|dosage|intake|daily|once daily|b\\.i\\.d\\.|t\\.i\\.d\\.)\\b|\\b(take|administer|prescribe|give|dosage|intake)\\b.*\\b\\d+\\s*(mg|ml|mcg|iu|units|g)\\b).*"
    );

    // 2. Drug formulary pattern
    private static final Pattern FORMULARY_PATTERN = Pattern.compile(
            "(?is).*\\b(drug|medicine|brand|generic|metformin|atorvastatin|amlodipine)\\b.*\\b(dose|dosage|frequency|route|administer|bid|tid|od)\\b.*"
    );

    // 3. Prescribing protocol language pattern
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile(
            "(?is).*\\b(first-line therapy|second-line treatment|dosing schedule|titrate|start at|initial dose|maintenance dose|recommended dosage)\\b.*"
    );

    public IngestionResult detectAndFilter(DocumentStructure structure, String documentName) {
        log.info("Stage 5 - PrescriptiveContentDetector: Scanning document structure tree for '{}'.", documentName);

        List<StructuredNode> safeNodes = new ArrayList<>();
        List<String> excludedLog = new ArrayList<>();
        int[] counts = new int[]{0, 0}; // totalScanned, totalExcluded

        for (StructuredNode rootNode : structure.getRootNodes()) {
            processNodeRecursive(rootNode, safeNodes, excludedLog, counts);
        }

        log.info("Stage 5 - PrescriptiveContentDetector completed for '{}'. Scanned {} nodes, Excluded {} prescriptive nodes, Safe nodes: {}.",
                documentName, counts[0], counts[1], safeNodes.size());

        return IngestionResult.builder()
                .documentName(documentName)
                .safeNodes(safeNodes)
                .excludedNodesLog(excludedLog)
                .totalNodesScanned(counts[0])
                .excludedNodeCount(counts[1])
                .build();
    }

    private void processNodeRecursive(StructuredNode node, List<StructuredNode> safeNodes, List<String> excludedLog, int[] counts) {
        counts[0]++; // increment scanned count

        String content = node.getTextContent();
        String matchReason = findPrescriptivePatternMatch(content);

        if (matchReason != null) {
            counts[1]++; // increment excluded count
            String logEntry = String.format("EXCLUDED node at path [%s] (Level: %s). Reason: Matched pattern '%s'. Content snippet: '%s'",
                    node.getPositionPath(), node.getLevel(), matchReason, truncateContent(content, 100));

            log.warn("Stage 5 AUDIT EXCLUSION: {}", logEntry);
            excludedLog.add(logEntry);
        } else {
            // Safe node
            List<StructuredNode> safeChildren = new ArrayList<>();
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                for (StructuredNode child : node.getChildren()) {
                    processNodeRecursive(child, safeChildren, excludedLog, counts);
                }
            }

            StructuredNode safeNode = StructuredNode.builder()
                    .level(node.getLevel())
                    .heading(node.getHeading())
                    .textContent(node.getTextContent())
                    .positionPath(node.getPositionPath())
                    .children(safeChildren)
                    .build();

            safeNodes.add(safeNode);
        }
    }

    public String detectPattern(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        if (DOSAGE_DIRECTIVE_PATTERN.matcher(content).matches()) {
            return "dosage_directive";
        }
        if (FORMULARY_PATTERN.matcher(content).matches()) {
            return "drug_table";
        }
        if (PROTOCOL_PATTERN.matcher(content).matches()) {
            return "protocol_directive";
        }
        return null;
    }

    public boolean isPrescriptive(String content) {
        return detectPattern(content) != null;
    }

    private String findPrescriptivePatternMatch(String content) {
        return detectPattern(content);
    }

    private String truncateContent(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
