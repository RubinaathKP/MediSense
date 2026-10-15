package com.medisense.rag.chunking;

import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.StructuredNode;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.CharacterApproximationTokenCounter;
import com.medisense.rag.chunking.token.TokenCounterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HierarchicalSemanticChunkerTest {

    private HierarchicalSemanticChunker chunker;
    private DomainTagger domainTagger;
    private ChunkQualityGate2 qualityGate2;

    @BeforeEach
    void setUp() {
        TokenCounterStrategy tokenCounter = new CharacterApproximationTokenCounter();
        domainTagger = new DomainTagger();
        qualityGate2 = new ChunkQualityGate2();
        chunker = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);
    }

    @Test
    void testSectionBoundaryEnforcement() {
        StructuredNode section1 = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("1.1 Lipid Profile Guidelines")
                .textContent("Elevated low density lipoprotein is a primary risk factor for atherosclerosis. Regular screening of lipid profiles is strongly recommended for adult health monitoring. Lipid management includes dietary and lifestyle interventions.")
                .positionPath("1.1")
                .children(new ArrayList<>())
                .build();

        StructuredNode section2 = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("1.2 Hypertension Management")
                .textContent("Blood pressure elevation increases cardiovascular disease risk. Regular blood pressure measurement is essential for adult health evaluations. Systolic and diastolic readings guide non-pharmacological lifestyle advice.")
                .positionPath("1.2")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(section1, section2))
                .totalNodeCount(2)
                .build();

        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure, UUID.randomUUID(), "ICMR_Guidelines.pdf", 2026, EvidenceBasis.CLINICAL_TRIAL_BACKED, "CORPUS", null
        );

        assertNotNull(chunkPairs);
        assertEquals(2, chunkPairs.size());

        // Verify child chunks of section 1 do not contain text from section 2
        ChunkPair pair1 = chunkPairs.get(0);
        assertEquals("1.1 Lipid Profile Guidelines", pair1.getParentChunk().getSectionHeading());
        for (ChildChunk child : pair1.getChildChunks()) {
            assertEquals("1.1 Lipid Profile Guidelines", child.getSectionHeading());
            assertFalse(child.getChunkText().contains("Hypertension Management"));
        }

        ChunkPair pair2 = chunkPairs.get(1);
        assertEquals("1.2 Hypertension Management", pair2.getParentChunk().getSectionHeading());
        for (ChildChunk child : pair2.getChildChunks()) {
            assertEquals("1.2 Hypertension Management", child.getSectionHeading());
            assertFalse(child.getChunkText().contains("Lipid Profile Guidelines"));
        }
    }

    @Test
    void testSentenceBoundarySplitting() {
        String text = "First sentence of the clinical section. Second sentence containing detailed medical explanation. Third sentence providing further context. Fourth sentence concluding the section nicely. Fifth sentence adding more clinical observations.";
        StructuredNode node = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("Sentence Test")
                .textContent(text)
                .positionPath("1.1")
                .build();

        DocumentStructure structure = new DocumentStructure(List.of(node), 1);

        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure, UUID.randomUUID(), "SentenceDoc.pdf", 2026, EvidenceBasis.CLINICAL_TRIAL_BACKED, "CORPUS", null
        );

        assertFalse(chunkPairs.isEmpty());
        for (ChildChunk child : chunkPairs.get(0).getChildChunks()) {
            String chunkText = child.getChunkText();
            assertTrue(chunkText.endsWith(".") || chunkText.endsWith("!") || chunkText.endsWith("?"));
        }
    }

    @Test
    void testTokenCountEnforcementInQualityGate2() {
        // Child chunk with < 50 tokens should be rejected by QualityGate2
        ChildChunk shortChunk = ChildChunk.builder()
                .chunkId(UUID.randomUUID())
                .chunkText("Too short.")
                .tokenCount(10)
                .isTable(false)
                .build();

        assertFalse(qualityGate2.validateChildChunk(shortChunk));

        // Child chunk with > 512 tokens should be rejected by QualityGate2
        ChildChunk hugeChunk = ChildChunk.builder()
                .chunkId(UUID.randomUUID())
                .chunkText("Long text snippet. ".repeat(200))
                .tokenCount(600)
                .isTable(false)
                .build();

        assertFalse(qualityGate2.validateChildChunk(hugeChunk));
    }

    @Test
    void testPrescriptiveContentDetectionAtChunkLevel() {
        ChildChunk prescriptiveChunk = ChildChunk.builder()
                .chunkId(UUID.randomUUID())
                .chunkText("Take 20mg of Atorvastatin daily at bedtime for initial cholesterol reduction.")
                .tokenCount(80)
                .isTable(false)
                .build();

        // Must be rejected by ChunkQualityGate2
        assertFalse(qualityGate2.validateChildChunk(prescriptiveChunk));
    }

    @Test
    void testTableChunkingHandling() {
        String tableJson = "{\"type\":\"extracted_table\",\"rows\":[\"Drug\\tDose\",\"Statins\\t20mg\"]}";

        DocumentStructure structure = new DocumentStructure(new ArrayList<>(), 0);

        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure, UUID.randomUUID(), "TableDoc.pdf", 2026, EvidenceBasis.CLINICAL_TRIAL_BACKED, "CORPUS", List.of(tableJson)
        );

        assertFalse(chunkPairs.isEmpty());
        ChildChunk tableChild = chunkPairs.get(0).getChildChunks().get(0);
        assertTrue(tableChild.isTable());
        assertEquals("Extracted Structured Table", tableChild.getSectionHeading());
    }

    @Test
    void testDomainTaggingAccuracy() {
        String text = "Patients with diabetes and hypertension require lipid management including low density lipoprotein testing and glycemic control.";
        List<String> tags = domainTagger.tagChunk(text);

        assertNotNull(tags);
        assertTrue(tags.contains("lipid_management"));
        assertTrue(tags.contains("diabetes_management"));
        assertTrue(tags.contains("hypertension"));
    }
}
