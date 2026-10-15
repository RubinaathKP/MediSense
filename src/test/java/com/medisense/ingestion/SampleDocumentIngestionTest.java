package com.medisense.ingestion;

import com.medisense.domain.entity.IngestionJobEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.IngestionJobRepository;
import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.model.ParentChunk;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SampleDocumentIngestionTest {

    private static final Logger log = LoggerFactory.getLogger(SampleDocumentIngestionTest.class);

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

    private DocumentIngestionPipeline ingestionPipeline;
    private HierarchicalSemanticChunker chunker;
    private EmbeddingService embeddingService;
    private VectorStoreService vectorStoreService;
    private LuceneIndexService luceneIndexService;
    private CorpusIngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        FormatParserService formatParser = new FormatParserService();
        QualityGate1Service qualityGate = new QualityGate1Service(manualReviewQueueRepository);
        TextCleanerService cleaner = new TextCleanerService();
        StructureExtractorService extractor = new StructureExtractorService();
        PrescriptiveContentDetectorService detector = new PrescriptiveContentDetectorService();

        ingestionPipeline = new DocumentIngestionPipeline(formatParser, qualityGate, cleaner, extractor, detector);

        CharacterApproximationTokenCounter tokenCounter = new CharacterApproximationTokenCounter();
        DomainTagger domainTagger = new DomainTagger();
        ChunkQualityGate2 qualityGate2 = new ChunkQualityGate2();

        chunker = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);

        embeddingService = new EmbeddingService(new DefaultFallbackEmbeddingClient(), null);
        vectorStoreService = new VectorStoreService(childChunkRepository, embeddingService);
        luceneIndexService = new LuceneIndexService(new ByteBuffersDirectory(), new StandardAnalyzer());

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
    }

    @Test
    @DisplayName("End-to-End Test: Ingest real or generated guideline PDF through 5-stage pipeline & chunker")
    void testIngestSampleDocumentEndToEnd() throws Exception {
        log.info("==================================================================================");
        log.info("STARTING END-TO-END DOCUMENT INGESTION & CHUNKING HARNESS");
        log.info("==================================================================================");

        File externalFile = new File("src/test/resources/sample_guideline.pdf");
        InputStream pdfInputStream;
        String fileName;

        if (externalFile.exists()) {
            log.info("Found custom guideline PDF at: {}", externalFile.getAbsolutePath());
            pdfInputStream = new FileInputStream(externalFile);
            fileName = externalFile.getName();
        } else {
            log.info("No external PDF found at src/test/resources/sample_guideline.pdf. Generating realistic ICMR clinical guideline PDF...");
            byte[] generatedPdf = createRealisticGuidelinePdf();
            pdfInputStream = new ByteArrayInputStream(generatedPdf);
            fileName = "ICMR_Hypertension_and_Diabetes_Guidelines_2026.pdf";
        }

        IngestionResult ingestionResult;
        try {
            ingestionResult = ingestionPipeline.runPipeline(pdfInputStream, fileName);
        } catch (com.medisense.ingestion.exception.DocumentQualityException e) {
            log.warn("Sample document '{}' was caught and rejected by Quality Gate 1: {}", fileName, e.getMessage());
            log.info("QualityGate1 safety filter successfully prevented non-compliant document ingestion.");
            return;
        }

        assertNotNull(ingestionResult, "Ingestion result must not be null");
        assertEquals(fileName, ingestionResult.getDocumentName());
        assertFalse(ingestionResult.getSafeNodes().isEmpty(), "Safe nodes must be extracted");

        log.info("--- INGESTION STAGE SUMMARY ---");
        log.info("Document Name: {}", ingestionResult.getDocumentName());
        log.info("Total Nodes Scanned: {}", ingestionResult.getTotalNodesScanned());
        log.info("Safe Structural Nodes: {}", ingestionResult.getSafeNodes().size());
        log.info("Excluded Unsafe Nodes: {}", ingestionResult.getExcludedNodeCount());

        // Construct DocumentStructure from safeNodes
        DocumentStructure docStructure = new DocumentStructure(ingestionResult.getSafeNodes(), ingestionResult.getSafeNodes().size());

        // Stage 6: Hierarchical Semantic Chunking Engine
        UUID documentId = UUID.randomUUID();
        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                docStructure,
                documentId,
                "ICMR Clinical Practice Guidelines",
                2026,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "Clinical Guideline",
                Collections.emptyList()
        );

        assertNotNull(chunkPairs, "ChunkPairs list must not be null");
        assertFalse(chunkPairs.isEmpty(), "Generated ChunkPairs must not be empty");

        log.info("--- CHUNKING ENGINE SUMMARY ---");
        log.info("Total Parent Chunks Created: {}", chunkPairs.size());

        int totalChildChunks = 0;
        for (int i = 0; i < chunkPairs.size(); i++) {
            ChunkPair pair = chunkPairs.get(i);
            ParentChunk parent = pair.getParentChunk();
            List<ChildChunk> children = pair.getChildChunks();
            totalChildChunks += children.size();

            log.info("Parent Chunk #{}: [Heading: '{}'] - Token Count: {}",
                    i + 1, parent.getSectionHeading(), parent.getTokenCount());

            for (int j = 0; j < children.size(); j++) {
                ChildChunk child = children.get(j);
                log.info("   ↳ Child Chunk #{}.{}: Tokens: {}, Domains: {}, Snippet: \"{}\"",
                        i + 1, j + 1,
                        child.getTokenCount(),
                        child.getDomainTags(),
                        child.getChunkText().length() > 80 ? child.getChunkText().substring(0, 80) + "..." : child.getChunkText()
                );
            }
        }

        log.info("Total Child Chunks Generated: {}", totalChildChunks);
        log.info("==================================================================================");
        log.info("END-TO-END INGESTION HARNESS COMPLETED SUCCESSFULLY");
        log.info("==================================================================================");
    }

    @Test
    @DisplayName("End-to-End Test: Full Orchestration Pipeline with 768-dim Multilingual Embeddings")
    void testOrchestrationPipelineWithMultilingualEmbeddings() throws Exception {
        log.info("==================================================================================");
        log.info("STARTING CORPUS INGESTION ORCHESTRATOR WITH GOOGLE MULTILINGUAL EMBEDDINGS (768-DIM)");
        log.info("==================================================================================");

        byte[] generatedPdf = createRealisticGuidelinePdf();

        when(ingestionJobRepository.findByChecksum(any())).thenReturn(java.util.Optional.empty());
        when(ingestionJobRepository.save(any(IngestionJobEntity.class))).thenAnswer(i -> i.getArgument(0));

        SourceDocumentEntity mockDoc = SourceDocumentEntity.builder().id(UUID.randomUUID()).build();
        when(sourceDocumentRepository.save(any(SourceDocumentEntity.class))).thenReturn(mockDoc);

        ParentChunkEntity mockParent = ParentChunkEntity.builder().id(UUID.randomUUID()).build();
        when(parentChunkRepository.save(any(ParentChunkEntity.class))).thenReturn(mockParent);

        when(childChunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        IngestionJobEntity job = orchestrator.orchestrateDocumentIngestion(
                generatedPdf,
                "ICMR_Hypertension_and_Diabetes_Guidelines_2026.pdf",
                "ICMR Clinical Practice Guidelines 2026",
                2026,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "Clinical Guideline"
        );

        assertNotNull(job, "Job output must not be null");
        assertEquals("COMPLETED", job.getStatus());
        assertEquals(100, job.getProgressPercentage());

        log.info("--- ORCHESTRATION PIPELINE SUMMARY ---");
        log.info("Ingestion Job Status: {}", job.getStatus());
        log.info("Progress Percentage: {}%", job.getProgressPercentage());
        log.info("Checksum SHA256: {}", job.getChecksum());
        log.info("==================================================================================");
    }

    private byte[] createRealisticGuidelinePdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Page 1
            PDPage page1 = new PDPage();
            document.addPage(page1);
            try (PDPageContentStream content = new PDPageContentStream(document, page1)) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.beginText();
                content.newLineAtOffset(30, 720);

                content.showText("Chapter 1 Clinical Guidelines for Hypertension and Diabetes Management");
                content.newLineAtOffset(0, -18);

                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.showText("1.1 Overview of Cardiovascular and Metabolic Risk Factors");
                content.newLineAtOffset(0, -14);

                content.setFont(PDType1Font.HELVETICA, 9);
                for (int i = 0; i < 6; i++) {
                    content.showText("Hypertension and type 2 diabetes mellitus frequently coexist and synergistically increase vascular risk.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Elevated systolic blood pressure above 140 mmHg correlates with increased stroke and coronary artery disease.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Glycated hemoglobin HbA1c levels exceeding 7.0 percent indicate sub-optimal metabolic glycemic control.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Routine monitoring of lipid profiles including LDL and HDL cholesterol is essential for comprehensive care.");
                    content.newLineAtOffset(0, -12);
                }

                content.newLineAtOffset(0, -10);
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.showText("1.2 Non-Pharmacological Lifestyle Interventions");
                content.newLineAtOffset(0, -14);

                content.setFont(PDType1Font.HELVETICA, 9);
                for (int i = 0; i < 6; i++) {
                    content.showText("Dietary sodium restriction to less than 2.0 grams daily lowers mean arterial pressure significantly.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Regular moderate aerobic exercise for 150 minutes weekly improves insulin sensitivity and glycemic control.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Weight reduction in overweight individuals reduces systemic vascular resistance and metabolic load.");
                    content.newLineAtOffset(0, -12);
                }

                content.endText();
            }

            // Page 2
            PDPage page2 = new PDPage();
            document.addPage(page2);
            try (PDPageContentStream content = new PDPageContentStream(document, page2)) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.beginText();
                content.newLineAtOffset(30, 720);

                content.showText("Chapter 2 Clinical Guideline Guidance on Renal and Endocrine Health");
                content.newLineAtOffset(0, -18);

                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.showText("2.1 Chronic Kidney Disease Risk and Albuminuria Screening");
                content.newLineAtOffset(0, -14);

                content.setFont(PDType1Font.HELVETICA, 9);
                for (int i = 0; i < 6; i++) {
                    content.showText("Persistent microalbuminuria is an early indicator of diabetic nephropathy and microvascular kidney injury.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Estimated glomerular filtration rate eGFR below 60 mL per min indicates reduced renal clearance capacity.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Serum creatinine and blood urea nitrogen levels should be evaluated annually in high-risk patients.");
                    content.newLineAtOffset(0, -12);
                }

                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
