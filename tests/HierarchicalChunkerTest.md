# Test Specification: HierarchicalChunkerTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.rag.chunking.HierarchicalSemanticChunker`
- **Category:** Unit Test (RAG Knowledge Engine)
- **Framework:** JUnit 5, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/rag/chunking/HierarchicalChunkerUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.rag.chunking;

import com.medisense.UnitTestBase;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.StructuredNode;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.CharacterApproximationTokenCounter;
import com.medisense.rag.chunking.token.TokenCounterStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HierarchicalChunkerUnitTest extends UnitTestBase {

    private HierarchicalSemanticChunker chunker;

    @Override
    protected void onSetup() {
        TokenCounterStrategy tokenCounter = new CharacterApproximationTokenCounter();
        DomainTagger domainTagger = new DomainTagger();
        ChunkQualityGate2 qualityGate2 = new ChunkQualityGate2();
        chunker = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);
    }

    @Test
    @DisplayName("Should create distinct parent chunks without crossing section boundaries")
    void shouldEnforceSectionBoundaries() {
        StructuredNode section1 = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("1.1 Lipid Profile Reference")
                .textContent("Elevated low-density lipoprotein cholesterol is an established biomarker for vascular disease. Routine serum lipid profiles evaluate cardiovascular health.")
                .positionPath("1.1")
                .children(new ArrayList<>())
                .build();

        StructuredNode section2 = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("1.2 Hypertension Guidelines")
                .textContent("Elevated arterial blood pressure increases stroke and renal complication rates. Periodic BP monitoring is an essential clinical assessment.")
                .positionPath("1.2")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(section1, section2))
                .totalNodeCount(2)
                .build();

        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure,
                UUID.randomUUID(),
                "ICMR_Guidelines.pdf",
                2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "CORPUS",
                null
        );

        assertThat(chunkPairs).isNotEmpty();
        assertThat(chunkPairs)
                .anyMatch(pair -> pair.parentChunk().content().contains("1.1 Lipid Profile Reference"))
                .anyMatch(pair -> pair.parentChunk().content().contains("1.2 Hypertension Guidelines"));
    }

    @Test
    @DisplayName("Should produce child chunks with valid parent chunk foreign key associations")
    void shouldAssociateChildWithParentChunk() {
        StructuredNode node = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("Glycemic Targets")
                .textContent("Fasting blood sugar targets should be maintained between 80 to 130 mg/dL for non-pregnant adults. Postprandial values below 180 mg/dL prevent microvascular damage.")
                .positionPath("2.0")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(node))
                .totalNodeCount(1)
                .build();

        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure,
                UUID.randomUUID(),
                "Diabetes_Care.pdf",
                2023,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "CORPUS",
                null
        );

        assertThat(chunkPairs).isNotEmpty();
        ChunkPair pair = chunkPairs.get(0);
        assertThat(pair.parentChunk()).isNotNull();
        assertThat(pair.childChunks()).isNotEmpty();
        pair.childChunks().forEach(child -> {
            assertThat(child.parentChunkId()).isEqualTo(pair.parentChunk().id());
            assertThat(child.content()).isNotBlank();
        });
    }
}
```
