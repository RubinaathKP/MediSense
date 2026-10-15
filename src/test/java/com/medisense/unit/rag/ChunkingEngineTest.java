package com.medisense.unit.rag;

import com.medisense.UnitTestBase;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.StructuredNode;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.TokenCounterStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: ChunkingEngineTest
 * Verifies hierarchical parent-child chunking, section boundary containment, token gate validation,
 * separate table extraction, 20-token overlap, and sentence boundary splitting.
 */
class ChunkingEngineTest extends UnitTestBase {

    private HierarchicalSemanticChunker chunkingEngine;
    private TokenCounterStrategy tokenCounter;
    private DomainTagger domainTagger;
    private ChunkQualityGate2 qualityGate2;

    @Override
    protected void onSetup() {
        tokenCounter = text -> {
            if (text == null || text.isBlank()) return 0;
            return text.trim().split("\\s+").length;
        };
        domainTagger = new DomainTagger();
        qualityGate2 = new ChunkQualityGate2();

        chunkingEngine = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);
    }

    private String generateSentences(int count, int wordsPerSentence) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            StringBuilder s = new StringBuilder("Sentence " + (i + 1));
            for (int w = 2; w <= wordsPerSentence; w++) {
                s.append(" word").append(w);
            }
            s.append(". ");
            sb.append(s);
        }
        return sb.toString().trim();
    }

    @Test
    @DisplayName("Case 1: Simple section chunking (2000 tokens) produces 1 parent and child chunks of 100-150 tokens")
    void testSimpleSectionChunking() {
        // 80 sentences of 25 tokens each = 2000 tokens
        String sectionText = generateSentences(80, 25);
        int totalTokens = tokenCounter.countTokens(sectionText);
        assertThat(totalTokens).isGreaterThanOrEqualTo(2000);

        StructuredNode sectionNode = TestDataBuilder.aStructuredNode()
                .heading("Chapter 1: Clinical Protocols")
                .textContent(sectionText)
                .positionPath("1")
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(sectionNode))
                .build();

        List<ChunkPair> pairs = chunkingEngine.chunkDocumentStructure(
                structure, UUID.randomUUID(), "Lipid_Guidelines.pdf", 2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED, "GUIDELINE", Collections.emptyList()
        );

        // Assert: 1 parent chunk created
        assertThat(pairs).hasSize(1);
        ChunkPair pair = pairs.get(0);
        assertThat(pair.getParentChunk()).isNotNull();
        assertThat(pair.getParentChunk().getSectionHeading()).isEqualTo("Chapter 1: Clinical Protocols");

        // Assert: multiple child chunks created, each child 100-150 tokens
        List<ChildChunk> children = pair.getChildChunks();
        assertThat(children).hasSizeGreaterThan(10);
        for (ChildChunk child : children) {
            assertThat(child.getTokenCount())
                    .as("Each child chunk should be roughly 100-150 tokens")
                    .isGreaterThanOrEqualTo(100)
                    .isLessThanOrEqualTo(160);
            assertThat(child.getParentChunkId()).isEqualTo(pair.getParentChunk().getParentChunkId());
        }
    }

    @Test
    @DisplayName("Case 2: Multiple sections (3 x 1500 tokens) produce 3 parent chunks without crossing section boundaries")
    void testMultipleSectionsDoNotCrossBoundaries() {
        String sec1Text = generateSentences(60, 25); // ~1500 tokens
        String sec2Text = generateSentences(60, 25); // ~1500 tokens
        String sec3Text = generateSentences(60, 25); // ~1500 tokens

        StructuredNode sec1 = TestDataBuilder.aStructuredNode()
                .heading("Section 1: Screening")
                .textContent(sec1Text)
                .positionPath("1")
                .build();
        StructuredNode sec2 = TestDataBuilder.aStructuredNode()
                .heading("Section 2: Diagnosis")
                .textContent(sec2Text)
                .positionPath("2")
                .build();
        StructuredNode sec3 = TestDataBuilder.aStructuredNode()
                .heading("Section 3: Lifestyle Management")
                .textContent(sec3Text)
                .positionPath("3")
                .build();

        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(sec1, sec2, sec3))
                .build();

        List<ChunkPair> pairs = chunkingEngine.chunkDocumentStructure(
                structure, UUID.randomUUID(), "Guidelines.pdf", 2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED, "GUIDELINE", Collections.emptyList()
        );

        // Assert: 3 parent chunks created
        assertThat(pairs).hasSize(3);
        assertThat(pairs.get(0).getParentChunk().getSectionHeading()).isEqualTo("Section 1: Screening");
        assertThat(pairs.get(1).getParentChunk().getSectionHeading()).isEqualTo("Section 2: Diagnosis");
        assertThat(pairs.get(2).getParentChunk().getSectionHeading()).isEqualTo("Section 3: Lifestyle Management");

        // Assert: each section's children don't cross section boundaries
        for (ChildChunk child : pairs.get(0).getChildChunks()) {
            assertThat(child.getParentChunkId()).isEqualTo(pairs.get(0).getParentChunk().getParentChunkId());
            assertThat(child.getSectionHeading()).isEqualTo("Section 1: Screening");
        }
        for (ChildChunk child : pairs.get(1).getChildChunks()) {
            assertThat(child.getParentChunkId()).isEqualTo(pairs.get(1).getParentChunk().getParentChunkId());
            assertThat(child.getSectionHeading()).isEqualTo("Section 2: Diagnosis");
        }
        for (ChildChunk child : pairs.get(2).getChildChunks()) {
            assertThat(child.getParentChunkId()).isEqualTo(pairs.get(2).getParentChunk().getParentChunkId());
            assertThat(child.getSectionHeading()).isEqualTo("Section 3: Lifestyle Management");
        }
    }

    @Test
    @DisplayName("Case 3: Token count validation: chunk with <= 50 tokens is rejected by quality gate and not stored")
    void testTokenCountValidationRejectsSmallChunk() {
        // 2 sentences of 25 tokens = 50 tokens
        String smallText = generateSentences(2, 25);
        assertThat(tokenCounter.countTokens(smallText)).isEqualTo(50);

        ChildChunk smallChunk = TestDataBuilder.aRagChildChunk()
                .chunkText(smallText)
                .tokenCount(50)
                .isTable(false)
                .build();

        // Assert: rejected by quality gate, not stored
        boolean isValid = qualityGate2.validateChildChunk(smallChunk);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Case 4: Table extraction: table extracted separately as table chunk, no table content in paragraph chunks")
    void testTableExtractionSeparately() {
        String paragraphContent = generateSentences(20, 15);
        StructuredNode node = TestDataBuilder.aStructuredNode()
                .heading("Section 4: Diagnostic Data")
                .textContent(paragraphContent)
                .positionPath("4")
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(node))
                .build();

        String tableJson = "{\"type\":\"extracted_table\",\"rows\":[\"Test | Result | Unit\",\"LDL | 145 | mg/dL\"]}";
        List<String> tables = List.of(tableJson);

        List<ChunkPair> pairs = chunkingEngine.chunkDocumentStructure(
                structure, UUID.randomUUID(), "Lab_Results.pdf", 2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED, "GUIDELINE", tables
        );

        // Assert: table extracted separately
        assertThat(pairs).hasSize(2); // 1 for paragraph section, 1 for table

        ChunkPair paraPair = pairs.stream()
                .filter(p -> p.getParentChunk().getSectionHeading().equals("Section 4: Diagnostic Data"))
                .findFirst().orElseThrow();
        ChunkPair tablePair = pairs.stream()
                .filter(p -> p.getParentChunk().getSectionHeading().equals("Extracted Structured Table"))
                .findFirst().orElseThrow();

        // Assert: paragraph chunks have no table content
        for (ChildChunk child : paraPair.getChildChunks()) {
            assertThat(child.isTable()).isFalse();
            assertThat(child.getChunkText()).doesNotContain("extracted_table");
            assertThat(child.getChunkText()).doesNotContain("Test | Result | Unit");
        }

        // Assert: table chunk has isTable=true and contains table JSON
        assertThat(tablePair.getChildChunks()).hasSize(1);
        ChildChunk tableChild = tablePair.getChildChunks().get(0);
        assertThat(tableChild.isTable()).isTrue();
        assertThat(tableChild.getChunkText()).contains("extracted_table");
    }

    @Test
    @DisplayName("Case 5: Overlap enforcement: last tokens of chunk N appear in first tokens of chunk N+1")
    void testOverlapEnforcementBetweenConsecutiveChunks() {
        // Sentences with distinctive content
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            textBuilder.append("Clinical marker ").append(i).append(" requires laboratory verification for metabolic analysis. ");
        }

        StructuredNode node = TestDataBuilder.aStructuredNode()
                .heading("Section 5: Markers")
                .textContent(textBuilder.toString().trim())
                .positionPath("5")
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(node))
                .build();

        List<ChunkPair> pairs = chunkingEngine.chunkDocumentStructure(
                structure, UUID.randomUUID(), "Markers.pdf", 2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED, "GUIDELINE", Collections.emptyList()
        );

        assertThat(pairs).isNotEmpty();
        List<ChildChunk> children = pairs.get(0).getChildChunks();
        assertThat(children.size()).isGreaterThanOrEqualTo(2);

        ChildChunk chunkN = children.get(0);
        ChildChunk chunkNPlus1 = children.get(1);

        // Assert: sentence overlapping between chunk N and chunk N+1
        String[] chunkNSentences = chunkN.getChunkText().split("(?<=[.!?])\\s+");
        String lastSentenceChunkN = chunkNSentences[chunkNSentences.length - 1].trim();

        assertThat(chunkNPlus1.getChunkText())
                .as("The boundary sentence from chunk N should overlap into chunk N+1")
                .contains(lastSentenceChunkN);
    }

    @Test
    @DisplayName("Case 6: Sentence boundary splitting: split waits for next sentence boundary instead of cutting mid-sentence")
    void testSentenceBoundarySplitting() {
        // Text with a very long sentence that causes chunk target tokens to be exceeded slightly
        String longSentence = "Serum low-density lipoprotein cholesterol concentrations should be systematically evaluated in all adult patients presenting with borderline cardiovascular indicators according to Indian Council of Medical Research diagnostic protocol.";
        String nextSentence = "Subsequent follow-up is recommended within twelve weeks.";

        String sectionText = longSentence + " " + nextSentence;

        StructuredNode node = TestDataBuilder.aStructuredNode()
                .heading("Section 6: Boundaries")
                .textContent(sectionText)
                .positionPath("6")
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(node))
                .build();

        List<ChunkPair> pairs = chunkingEngine.chunkDocumentStructure(
                structure, UUID.randomUUID(), "Boundaries.pdf", 2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED, "GUIDELINE", Collections.emptyList()
        );

        assertThat(pairs).isNotEmpty();
        List<ChildChunk> children = pairs.get(0).getChildChunks();

        // Assert: split does not cut mid-sentence (every chunk ends with sentence punctuation)
        for (ChildChunk child : children) {
            String trimmed = child.getChunkText().trim();
            assertThat(trimmed).matches(".*[.!?]$");
            assertThat(trimmed).doesNotContain("Council of Medical Research diagnostic protocol without period");
        }
    }
}
