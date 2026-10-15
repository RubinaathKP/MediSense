package com.medisense.rag.chunking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.StructuredNode;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.model.ParentChunk;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.TokenCounterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class HierarchicalSemanticChunker {

    private static final Logger log = LoggerFactory.getLogger(HierarchicalSemanticChunker.class);

    private static final int TARGET_CHILD_TOKENS = 125; // Target 100-150 tokens
    private static final int OVERLAP_TOKENS = 20;      // 20-token overlap between consecutive child chunks

    // Sentence boundary regex (. ! ?)
    private static final Pattern SENTENCE_BOUNDARY_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    private final TokenCounterStrategy tokenCounter;
    private final DomainTagger domainTagger;
    private final ChunkQualityGate2 qualityGate2;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HierarchicalSemanticChunker(TokenCounterStrategy tokenCounter,
                                        DomainTagger domainTagger,
                                        ChunkQualityGate2 qualityGate2) {
        this.tokenCounter = tokenCounter;
        this.domainTagger = domainTagger;
        this.qualityGate2 = qualityGate2;
    }

    public List<ChunkPair> chunkDocumentStructure(DocumentStructure structure,
                                                 UUID docId,
                                                 String sourceName,
                                                 int sourceYear,
                                                 EvidenceBasis evidenceBasis,
                                                 String sourceType,
                                                 List<String> extractedTablesJson) {
        log.info("Starting Hierarchical Semantic Chunking for document '{}' (ID: {}).", sourceName, docId);

        List<ChunkPair> chunkPairs = new ArrayList<>();
        int sectionIndex = 0;

        // 1. Process document section hierarchy
        for (StructuredNode rootNode : structure.getRootNodes()) {
            sectionIndex = processNodeSection(rootNode, docId, sourceName, sourceYear, evidenceBasis, sourceType, chunkPairs, sectionIndex);
        }

        // 2. Process extracted tables separately as single child chunks (never split)
        if (extractedTablesJson != null && !extractedTablesJson.isEmpty()) {
            processTables(extractedTablesJson, docId, sourceName, sourceYear, evidenceBasis, sourceType, chunkPairs, sectionIndex);
        }

        log.info("Finished Hierarchical Semantic Chunking for document '{}'. Generated {} ChunkPairs.", sourceName, chunkPairs.size());
        return chunkPairs;
    }

    private int processNodeSection(StructuredNode node,
                                  UUID docId,
                                  String sourceName,
                                  int sourceYear,
                                  EvidenceBasis evidenceBasis,
                                  String sourceType,
                                  List<ChunkPair> chunkPairs,
                                  int sectionIndex) {

        String sectionHeading = node.getHeading() != null ? node.getHeading() : ("Section " + node.getPositionPath());
        String sectionText = node.getTextContent();

        if (sectionText != null && !sectionText.isBlank()) {
            UUID parentId = UUID.randomUUID();
            int parentTokens = tokenCounter.countTokens(sectionText);

            ParentChunk parentChunk = ParentChunk.builder()
                    .parentChunkId(parentId)
                    .docId(docId)
                    .sourceName(sourceName)
                    .sectionHeading(sectionHeading)
                    .parentText(sectionText)
                    .tokenCount(parentTokens)
                    .sectionIndex(sectionIndex++)
                    .build();

            // Create child chunks for this parent section (strictly enforcing section boundary)
            List<ChildChunk> childChunks = createChildChunksForSection(
                    sectionText, parentId, docId, sourceName, sourceYear, evidenceBasis, sourceType, sectionHeading
            );

            if (!childChunks.isEmpty()) {
                chunkPairs.add(new ChunkPair(parentChunk, childChunks));
            }
        }

        if (node.getChildren() != null) {
            for (StructuredNode childNode : node.getChildren()) {
                sectionIndex = processNodeSection(childNode, docId, sourceName, sourceYear, evidenceBasis, sourceType, chunkPairs, sectionIndex);
            }
        }

        return sectionIndex;
    }

    private List<ChildChunk> createChildChunksForSection(String sectionText,
                                                        UUID parentId,
                                                        UUID docId,
                                                        String sourceName,
                                                        int sourceYear,
                                                        EvidenceBasis evidenceBasis,
                                                        String sourceType,
                                                        String sectionHeading) {

        List<ChildChunk> childChunks = new ArrayList<>();

        // Split text ONLY on sentence boundaries
        String[] sentences = SENTENCE_BOUNDARY_PATTERN.split(sectionText.trim());
        List<String> currentSentences = new ArrayList<>();
        int currentTokens = 0;
        int childIndex = 0;

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.isBlank()) continue;

            int sentenceTokens = tokenCounter.countTokens(sentence);
            currentSentences.add(sentence);
            currentTokens += sentenceTokens;

            if (currentTokens >= TARGET_CHILD_TOKENS || i == sentences.length - 1) {
                String chunkText = String.join(" ", currentSentences);
                int chunkTokens = tokenCounter.countTokens(chunkText);

                List<String> domainTags = domainTagger.tagChunk(chunkText);

                ChildChunk childChunk = ChildChunk.builder()
                        .chunkId(UUID.randomUUID())
                        .parentChunkId(parentId)
                        .docId(docId)
                        .sourceName(sourceName)
                        .sourceYear(sourceYear)
                        .evidenceBasis(evidenceBasis != null ? evidenceBasis : EvidenceBasis.CLINICAL_TRIAL_BACKED)
                        .sourceType(sourceType)
                        .sectionHeading(sectionHeading)
                        .domainTags(domainTags)
                        .chunkText(chunkText)
                        .tokenCount(chunkTokens)
                        .chunkIndex(childIndex++)
                        .language("en")
                        .isTable(false)
                        .tableHeader(Collections.emptyList())
                        .build();

                // Validate through ChunkQualityGate2 (50-512 token range & chunk-level prescriptive check)
                if (qualityGate2.validateChildChunk(childChunk)) {
                    childChunks.add(childChunk);
                }

                // Handle 20-token overlap: retain last sentence for next chunk
                currentSentences.clear();
                currentTokens = 0;
                if (i < sentences.length - 1 && sentenceTokens <= OVERLAP_TOKENS + 10) {
                    currentSentences.add(sentence);
                    currentTokens = sentenceTokens;
                }
            }
        }

        return childChunks;
    }

    private void processTables(List<String> tablesJson,
                              UUID docId,
                              String sourceName,
                              int sourceYear,
                              EvidenceBasis evidenceBasis,
                              String sourceType,
                              List<ChunkPair> chunkPairs,
                              int sectionIndex) {

        for (String json : tablesJson) {
            UUID parentId = UUID.randomUUID();
            int tableTokens = tokenCounter.countTokens(json);

            ParentChunk parentChunk = ParentChunk.builder()
                    .parentChunkId(parentId)
                    .docId(docId)
                    .sourceName(sourceName)
                    .sectionHeading("Extracted Structured Table")
                    .parentText(json)
                    .tokenCount(tableTokens)
                    .sectionIndex(sectionIndex++)
                    .build();

            List<String> headers = parseTableHeader(json);
            List<String> domainTags = domainTagger.tagChunk(json);

            ChildChunk tableChildChunk = ChildChunk.builder()
                    .chunkId(UUID.randomUUID())
                    .parentChunkId(parentId)
                    .docId(docId)
                    .sourceName(sourceName)
                    .sourceYear(sourceYear)
                    .evidenceBasis(evidenceBasis != null ? evidenceBasis : EvidenceBasis.CLINICAL_TRIAL_BACKED)
                    .sourceType(sourceType)
                    .sectionHeading("Extracted Structured Table")
                    .domainTags(domainTags)
                    .chunkText(json)
                    .tokenCount(tableTokens)
                    .chunkIndex(0)
                    .language("en")
                    .isTable(true)
                    .tableHeader(headers)
                    .build();

            if (qualityGate2.validateChildChunk(tableChildChunk)) {
                chunkPairs.add(new ChunkPair(parentChunk, List.of(tableChildChunk)));
            }
        }
    }

    private List<String> parseTableHeader(String tableJson) {
        try {
            var node = objectMapper.readTree(tableJson);
            if (node.has("rows") && node.get("rows").isArray() && !node.get("rows").isEmpty()) {
                String firstRow = node.get("rows").get(0).asText();
                return Arrays.asList(firstRow.split("[\\t|,]"));
            }
        } catch (Exception e) {
            log.trace("Table header parsing skipped for JSON: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
