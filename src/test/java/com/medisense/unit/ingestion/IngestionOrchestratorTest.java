package com.medisense.unit.ingestion;

import com.medisense.UnitTestBase;
import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.entity.IngestionJobEntity;
import com.medisense.domain.entity.ManualReviewQueueEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.IngestionJobRepository;
import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.ingestion.CorpusIngestionOrchestrator;
import com.medisense.ingestion.DocumentIngestionPipeline;
import com.medisense.ingestion.FormatParserService;
import com.medisense.ingestion.PrescriptiveContentDetectorService;
import com.medisense.ingestion.QualityGate1Service;
import com.medisense.ingestion.StructureExtractorService;
import com.medisense.ingestion.TextCleanerService;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.ingestion.model.StructuredNode;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.model.ParentChunk;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.TokenCounterStrategy;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.embedding.VectorStoreService;
import com.medisense.rag.retrieval.LuceneIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test: IngestionOrchestratorTest
 * Integration-style unit tests verifying end-to-end orchestration across all 5 stages of document ingestion.
 */
class IngestionOrchestratorTest extends UnitTestBase {

    @Mock
    private SourceDocumentRepository sourceDocumentRepository;

    @Mock
    private ParentChunkRepository parentChunkRepository;

    @Mock
    private ChildChunkRepository childChunkRepository;

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @Mock
    private ManualReviewQueueRepository manualReviewQueueRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private LuceneIndexService luceneIndexService;

    private DocumentIngestionPipeline ingestionPipeline;
    private HierarchicalSemanticChunker chunker;
    private CorpusIngestionOrchestrator orchestrator;

    private QualityGate1Service qualityGate1Service;
    private TextCleanerService textCleanerService;
    private StructureExtractorService structureExtractorService;
    private PrescriptiveContentDetectorService prescriptiveDetectorService;
    private FormatParserService formatParserService;

    private final List<ChildChunkEntity> storedChildChunks = new ArrayList<>();

    @Override
    protected void onSetup() {
        storedChildChunks.clear();

        formatParserService = new FormatParserService();
        qualityGate1Service = new QualityGate1Service(manualReviewQueueRepository);
        textCleanerService = new TextCleanerService();
        structureExtractorService = new StructureExtractorService();
        prescriptiveDetectorService = new PrescriptiveContentDetectorService();

        ingestionPipeline = new DocumentIngestionPipeline(
                formatParserService,
                qualityGate1Service,
                textCleanerService,
                structureExtractorService,
                prescriptiveDetectorService
        );

        TokenCounterStrategy tokenCounter = text -> text.split("\\s+").length;
        DomainTagger domainTagger = new DomainTagger();
        ChunkQualityGate2 qualityGate2 = new ChunkQualityGate2();
        chunker = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);

        orchestrator = new CorpusIngestionOrchestrator(
                ingestionPipeline,
                chunker,
                embeddingService,
                vectorStoreService,
                luceneIndexService,
                sourceDocumentRepository,
                parentChunkRepository,
                childChunkRepository,
                ingestionJobRepository
        );

        lenient().when(sourceDocumentRepository.save(any(SourceDocumentEntity.class))).thenAnswer(inv -> {
            SourceDocumentEntity doc = inv.getArgument(0);
            if (doc.getId() == null) doc.setId(UUID.randomUUID());
            return doc;
        });

        lenient().when(parentChunkRepository.save(any(ParentChunkEntity.class))).thenAnswer(inv -> {
            ParentChunkEntity pc = inv.getArgument(0);
            if (pc.getId() == null) pc.setId(UUID.randomUUID());
            return pc;
        });

        lenient().when(ingestionJobRepository.save(any(IngestionJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(embeddingService.generateEmbeddingsBatch(anyList())).thenAnswer(inv -> {
            List<String> texts = inv.getArgument(0);
            List<float[]> embeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                embeddings.add(new float[768]);
            }
            return embeddings;
        });

        lenient().doAnswer(inv -> {
            List<?> batch = inv.getArgument(0);
            for (Object item : batch) {
                ChildChunkEntity entity = TestDataBuilder.aChildChunk().build();
                storedChildChunks.add(entity);
            }
            return null;
        }).when(vectorStoreService).storeChildChunksBatch(anyList());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 1: Full happy path: PDF -> parsed -> cleaned -> structured -> chunked -> embedded -> stored")
    void testFullHappyPathIngestion() throws Exception {
        File pdfFile = new File("src/test/resources/fixtures/pdfs/valid_lab_report.pdf");
        assertThat(pdfFile).exists();
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

        // Document is new, no existing job
        when(ingestionJobRepository.findByChecksum(anyString())).thenReturn(Optional.empty());

        // Execute orchestration
        IngestionJobEntity completedJob = orchestrator.orchestrateDocumentIngestion(
                pdfBytes,
                "valid_lab_report.pdf",
                "ICMR Lipid Lab Report",
                2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "CORPUS"
        );

        // Assert: Ingestion job completed successfully
        assertThat(completedJob).isNotNull();
        assertThat(completedJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedJob.getProgressPercentage()).isEqualTo(100);

        // Assert: Chunks stored and appear in child_chunks table/repository
        verify(vectorStoreService).storeChildChunksBatch(anyList());
        verify(luceneIndexService).indexChunksBatch(anyList());
        assertThat(storedChildChunks).isNotEmpty();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 2: Quality gate 1 rejection: image-only PDF rejected before cleaning, manual_review_queue entry created")
    void testQualityGate1RejectionImageOnlyPdf() throws Exception {
        File imageOnlyPdf = new File("src/test/resources/fixtures/pdfs/invalid_lab_report_image_only.pdf");
        assertThat(imageOnlyPdf).exists();
        byte[] pdfBytes = Files.readAllBytes(imageOnlyPdf.toPath());

        when(ingestionJobRepository.findByChecksum(anyString())).thenReturn(Optional.empty());

        // Assert: document rejected before cleaning, throws exception and creates manual_review_queue entry
        assertThatThrownBy(() -> orchestrator.orchestrateDocumentIngestion(
                pdfBytes,
                "invalid_lab_report_image_only.pdf",
                "Scanned Lab Document",
                2024,
                EvidenceBasis.TRADITIONAL_SYSTEM_RECOGNIZED,
                "CORPUS"
        )).isInstanceOf(RuntimeException.class);

        // Assert: manual_review_queue entry created
        ArgumentCaptor<ManualReviewQueueEntity> reviewCaptor = ArgumentCaptor.forClass(ManualReviewQueueEntity.class);
        verify(manualReviewQueueRepository, atLeastOnce()).save(reviewCaptor.capture());
        ManualReviewQueueEntity entry = reviewCaptor.getValue();
        assertThat(entry.getDocumentName()).isEqualTo("invalid_lab_report_image_only.pdf");
        assertThat(entry.getRejectionStage()).isEqualTo("QualityGate1");

        // Verify downstream stages were NOT executed
        verify(parentChunkRepository, never()).save(any());
        verify(vectorStoreService, never()).storeChildChunksBatch(anyList());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 3: Prescriptive content rejection: PDF with drug formulary rejected by safety detector and logged")
    void testPrescriptiveContentRejectionDrugFormulary() {
        StructuredNode safeNode = TestDataBuilder.aStructuredNode()
                .heading("Section 1 Introduction")
                .textContent("Serum cholesterol levels should be monitored routinely in diabetic patients.")
                .positionPath("1.1")
                .build();

        StructuredNode prescriptiveTableNode = TestDataBuilder.aStructuredNode()
                .heading("Section 2 Formulary Table")
                .textContent("Drug | Dosage | Frequency\nMetformin | 500mg | BID\nAtorvastatin | 20mg | OD")
                .positionPath("1.2")
                .build();

        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(safeNode, prescriptiveTableNode))
                .build();

        IngestionResult result = prescriptiveDetectorService.detectAndFilter(structure, "drug_formulary.pdf");

        // Assert: document accepted, but chunks with prescriptive content rejected and logged
        assertThat(result).isNotNull();
        assertThat(result.getSafeNodes()).hasSize(1);
        assertThat(result.getSafeNodes().get(0).getHeading()).isEqualTo("Section 1 Introduction");
        assertThat(result.getExcludedNodeCount()).isEqualTo(1);
        assertThat(result.getExcludedNodesLog()).isNotEmpty();
        assertThat(result.getExcludedNodesLog().get(0))
                .contains("drug_table")
                .contains("EXCLUDED");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 4: Idempotent ingestion: same document ingested twice skips second run without duplicate chunks")
    void testIdempotentIngestionSkipsSecondRun() throws Exception {
        File pdfFile = new File("src/test/resources/fixtures/pdfs/valid_lab_report.pdf");
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

        IngestionJobEntity completedJob = IngestionJobEntity.builder()
                .documentName("valid_lab_report.pdf")
                .checksum("mock_checksum_valid")
                .status("COMPLETED")
                .progressPercentage(100)
                .build();

        // First run returns completed job, simulating already ingested document
        when(ingestionJobRepository.findByChecksum(anyString())).thenReturn(Optional.of(completedJob));

        IngestionJobEntity result = orchestrator.orchestrateDocumentIngestion(
                pdfBytes,
                "valid_lab_report.pdf",
                "ICMR Lab Report",
                2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "CORPUS"
        );

        // Assert: second run skipped, warning logged, no duplicate chunks
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        verify(sourceDocumentRepository, never()).save(any(SourceDocumentEntity.class));
        verify(vectorStoreService, never()).storeChildChunksBatch(anyList());
        verify(luceneIndexService, never()).indexChunksBatch(anyList());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 5: Transaction rollback: embedding succeeds but storage fails, document marked FAILED")
    void testTransactionRollbackOnStorageFailure() throws Exception {
        File pdfFile = new File("src/test/resources/fixtures/pdfs/valid_lab_report.pdf");
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());

        when(ingestionJobRepository.findByChecksum(anyString())).thenReturn(Optional.empty());

        // Simulate storage failure after embedding succeeds
        doThrow(new RuntimeException("Database vector store persistence error: connection reset"))
                .when(vectorStoreService).storeChildChunksBatch(anyList());

        // Assert: exception thrown and job marked FAILED
        assertThatThrownBy(() -> orchestrator.orchestrateDocumentIngestion(
                pdfBytes,
                "valid_lab_report.pdf",
                "ICMR Lab Report",
                2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "CORPUS"
        )).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Orchestration pipeline execution failed");

        // Verify job entity status updated to FAILED
        ArgumentCaptor<IngestionJobEntity> jobCaptor = ArgumentCaptor.forClass(IngestionJobEntity.class);
        verify(ingestionJobRepository, atLeastOnce()).save(jobCaptor.capture());

        List<IngestionJobEntity> savedJobs = jobCaptor.getAllValues();
        IngestionJobEntity lastSavedJob = savedJobs.get(savedJobs.size() - 1);
        assertThat(lastSavedJob.getStatus()).isEqualTo("FAILED");
        assertThat(lastSavedJob.getErrorMessage()).contains("Database vector store persistence error");
    }
}
