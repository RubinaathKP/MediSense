package com.medisense.ingestion;

import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.entity.IngestionJobEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.IngestionJobRepository;
import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.CharacterApproximationTokenCounter;
import com.medisense.rag.embedding.DefaultFallbackEmbeddingClient;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.embedding.VectorStoreService;
import com.medisense.rag.retrieval.LuceneIndexService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorpusIngestionOrchestratorTest {

    @Mock
    private ManualReviewQueueRepository manualReviewQueueRepository;

    @Mock
    private SourceDocumentRepository sourceDocumentRepository;

    @Mock
    private ParentChunkRepository parentChunkRepository;

    @Mock
    private ChildChunkRepository childChunkRepository;

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    private CorpusIngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        FormatParserService formatParser = new FormatParserService();
        QualityGate1Service qualityGate = new QualityGate1Service(manualReviewQueueRepository);
        TextCleanerService cleaner = new TextCleanerService();
        StructureExtractorService extractor = new StructureExtractorService();
        PrescriptiveContentDetectorService detector = new PrescriptiveContentDetectorService();

        DocumentIngestionPipeline pipeline = new DocumentIngestionPipeline(formatParser, qualityGate, cleaner, extractor, detector);

        CharacterApproximationTokenCounter tokenCounter = new CharacterApproximationTokenCounter();
        DomainTagger domainTagger = new DomainTagger();
        ChunkQualityGate2 qualityGate2 = new ChunkQualityGate2();

        HierarchicalSemanticChunker chunker = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);

        EmbeddingService embeddingService = new EmbeddingService(new DefaultFallbackEmbeddingClient(), null);
        VectorStoreService vectorStoreService = new VectorStoreService(childChunkRepository, embeddingService);
        LuceneIndexService luceneIndexService = new LuceneIndexService(new ByteBuffersDirectory(), new StandardAnalyzer());

        orchestrator = new CorpusIngestionOrchestrator(
                pipeline,
                chunker,
                embeddingService,
                vectorStoreService,
                luceneIndexService,
                sourceDocumentRepository,
                parentChunkRepository,
                childChunkRepository,
                ingestionJobRepository
        );
    }

    @Test
    @DisplayName("Test Idempotency: Skip re-ingestion if document checksum already exists with status COMPLETED")
    void testIdempotencySkipExistingCompletedDocument() throws Exception {
        byte[] pdfBytes = createSamplePdf();

        IngestionJobEntity completedJob = IngestionJobEntity.builder()
                .id(UUID.randomUUID())
                .documentName("ICMR_Hypertension.pdf")
                .checksum("test_checksum")
                .status("COMPLETED")
                .progressPercentage(100)
                .build();

        when(ingestionJobRepository.findByChecksum(anyString())).thenReturn(Optional.of(completedJob));

        IngestionJobEntity result = orchestrator.orchestrateDocumentIngestion(
                pdfBytes, "ICMR_Hypertension.pdf", "ICMR Guideline", 2026, EvidenceBasis.CLINICAL_TRIAL_BACKED, "Clinical Guideline"
        );

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(100, result.getProgressPercentage());
    }

    @Test
    @DisplayName("Test Full Orchestration Pipeline & Progress Tracking (0% -> 25% -> 50% -> 75% -> 100%)")
    void testFullOrchestrationPipeline() throws Exception {
        byte[] pdfBytes = createSamplePdf();

        when(ingestionJobRepository.findByChecksum(anyString())).thenReturn(Optional.empty());
        when(ingestionJobRepository.save(any(IngestionJobEntity.class))).thenAnswer(i -> i.getArgument(0));

        SourceDocumentEntity mockDoc = SourceDocumentEntity.builder().id(UUID.randomUUID()).build();
        when(sourceDocumentRepository.save(any(SourceDocumentEntity.class))).thenReturn(mockDoc);

        ParentChunkEntity mockParentEntity = ParentChunkEntity.builder().id(UUID.randomUUID()).build();
        when(parentChunkRepository.save(any(ParentChunkEntity.class))).thenReturn(mockParentEntity);

        when(childChunkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        IngestionJobEntity finalJob = orchestrator.orchestrateDocumentIngestion(
                pdfBytes, "ICMR_Hypertension.pdf", "ICMR Guideline", 2026, EvidenceBasis.CLINICAL_TRIAL_BACKED, "Clinical Guideline"
        );

        assertNotNull(finalJob);
        assertEquals("COMPLETED", finalJob.getStatus());
        assertEquals(100, finalJob.getProgressPercentage());

        verify(sourceDocumentRepository, times(1)).save(any(SourceDocumentEntity.class));
        verify(parentChunkRepository, atLeastOnce()).save(any(ParentChunkEntity.class));
        verify(childChunkRepository, times(1)).saveAll(anyList());
    }

    private byte[] createSamplePdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.beginText();
                content.newLineAtOffset(30, 700);

                content.showText("Chapter 1 Clinical Guidelines for Hypertension and Diabetes Management");
                content.newLineAtOffset(0, -14);
                for (int i = 0; i < 35; i++) {
                    content.showText("Hypertension and type 2 diabetes mellitus frequently coexist and synergistically increase vascular risk.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Elevated systolic blood pressure above 140 mmHg correlates with increased stroke and coronary artery disease.");
                    content.newLineAtOffset(0, -12);
                }

                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
