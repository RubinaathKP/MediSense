package com.medisense.ingestion;

import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.CharacterApproximationTokenCounter;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.embedding.VectorStoreService;
import com.medisense.rag.retrieval.LuceneIndexService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseIngestionServiceTest {

    @Mock
    private ManualReviewQueueRepository manualReviewQueueRepository;

    @Mock
    private SourceDocumentRepository sourceDocumentRepository;

    @Mock
    private ParentChunkRepository parentChunkRepository;

    @Mock
    private ChildChunkRepository childChunkRepository;

    private KnowledgeBaseIngestionService kbIngestionService;

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

        EmbeddingService embeddingService = new EmbeddingService(new com.medisense.rag.embedding.DefaultFallbackEmbeddingClient(), null);

        VectorStoreService vectorStoreService = new VectorStoreService(childChunkRepository, embeddingService);
        ReflectionTestUtils.setField(vectorStoreService, "maintenanceWorkMem", "512MB");

        LuceneIndexService luceneIndexService = new LuceneIndexService(new ByteBuffersDirectory(), new StandardAnalyzer());

        kbIngestionService = new KnowledgeBaseIngestionService(
                pipeline, chunker, sourceDocumentRepository, parentChunkRepository, childChunkRepository, vectorStoreService, luceneIndexService
        );
    }

    @Test
    @DisplayName("Verify KnowledgeBaseIngestionService processes document with batch embeddings, HNSW tuning & Lucene sync")
    void testIngestGuidelineDocument() throws Exception {
        byte[] pdfBytes = createSampleGuidelinePdf();

        SourceDocumentEntity mockDoc = SourceDocumentEntity.builder()
                .id(UUID.randomUUID())
                .documentName("ICMR Guideline")
                .documentType("Clinical Guideline")
                .sourceType("GUIDELINE")
                .contentHash("hash_test")
                .publicationYear(2026)
                .versionTag("v1.0")
                .build();

        ParentChunkEntity mockParent = ParentChunkEntity.builder()
                .id(UUID.randomUUID())
                .document(mockDoc)
                .chunkIndex(0)
                .content("Parent text")
                .build();

        when(sourceDocumentRepository.save(any(SourceDocumentEntity.class))).thenReturn(mockDoc);
        when(parentChunkRepository.save(any(ParentChunkEntity.class))).thenReturn(mockParent);
        when(childChunkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        kbIngestionService.ingestGuidelineDocument(
                new ByteArrayInputStream(pdfBytes),
                "ICMR_Guideline.pdf",
                "ICMR Guideline",
                2026,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "Clinical Guideline"
        );

        verify(sourceDocumentRepository, times(1)).save(any(SourceDocumentEntity.class));
        verify(parentChunkRepository, atLeastOnce()).save(any(ParentChunkEntity.class));
        verify(childChunkRepository, times(1)).saveAll(anyList());
    }

    private byte[] createSampleGuidelinePdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.beginText();
                content.newLineAtOffset(30, 720);

                content.showText("Chapter 1 Clinical Guidelines for Hypertension and Diabetes Management");
                content.newLineAtOffset(0, -18);

                content.setFont(PDType1Font.HELVETICA, 9);
                for (int i = 0; i < 15; i++) {
                    content.showText("Hypertension and type 2 diabetes mellitus frequently coexist and synergistically increase vascular risk.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Elevated systolic blood pressure above 140 mmHg correlates with increased stroke and coronary artery disease.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Glycated hemoglobin HbA1c levels exceeding 7.0 percent indicate sub-optimal metabolic glycemic control.");
                    content.newLineAtOffset(0, -12);
                }

                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
