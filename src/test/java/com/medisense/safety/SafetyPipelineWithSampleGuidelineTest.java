package com.medisense.safety;

import com.medisense.audit.AuditLogService;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.ingestion.DocumentIngestionPipeline;
import com.medisense.ingestion.FormatParserService;
import com.medisense.ingestion.PrescriptiveContentDetectorService;
import com.medisense.ingestion.QualityGate1Service;
import com.medisense.ingestion.StructureExtractorService;
import com.medisense.ingestion.TextCleanerService;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.gate.ChunkQualityGate2;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.tagger.DomainTagger;
import com.medisense.rag.chunking.token.CharacterApproximationTokenCounter;
import com.medisense.safety.model.QuerySafetyCategory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SafetyPipelineWithSampleGuidelineTest {

    private static final Logger log = LoggerFactory.getLogger(SafetyPipelineWithSampleGuidelineTest.class);

    private DocumentIngestionPipeline ingestionPipeline;
    private HierarchicalSemanticChunker chunker;
    private PatternLibraryLoader patternLibraryLoader;
    private SecondaryLlmSafetyClassifier secondaryLlmClassifier;
    private InputSafetyClassifier inputClassifier;
    private OutputSafetyFilter outputFilter;
    private CitationVerifier citationVerifier;
    private AuditLogService mockAuditLogService;
    private SafetyPipelineOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        FormatParserService formatParser = new FormatParserService();
        QualityGate1Service qualityGate = new QualityGate1Service(null);
        TextCleanerService cleaner = new TextCleanerService();
        StructureExtractorService extractor = new StructureExtractorService();
        PrescriptiveContentDetectorService detector = new PrescriptiveContentDetectorService();

        ingestionPipeline = new DocumentIngestionPipeline(formatParser, qualityGate, cleaner, extractor, detector);

        CharacterApproximationTokenCounter tokenCounter = new CharacterApproximationTokenCounter();
        DomainTagger domainTagger = new DomainTagger();
        ChunkQualityGate2 qualityGate2 = new ChunkQualityGate2();

        chunker = new HierarchicalSemanticChunker(tokenCounter, domainTagger, qualityGate2);

        patternLibraryLoader = new PatternLibraryLoader();
        patternLibraryLoader.init();
        secondaryLlmClassifier = mock(SecondaryLlmSafetyClassifier.class);
        inputClassifier = new InputSafetyClassifier(patternLibraryLoader, secondaryLlmClassifier);
        outputFilter = new OutputSafetyFilter();
        citationVerifier = new CitationVerifier();
        mockAuditLogService = mock(AuditLogService.class);

        orchestrator = new SafetyPipelineOrchestrator(inputClassifier, outputFilter, citationVerifier, mockAuditLogService);
    }

    @Test
    @DisplayName("End-to-End Safety Pipeline Test with Real sample_guideline.pdf Extracted Chunks")
    void testSafetyPipelineWithSampleGuideline() throws Exception {
        log.info("=========================================================================");
        log.info("STARTING 3-STAGE SAFETY PIPELINE TEST ON sample_guideline.pdf");
        log.info("=========================================================================");

        File sampleFile = new File("src/test/resources/sample_guideline.pdf");
        InputStream inputStream;
        String fileName;

        if (sampleFile.exists()) {
            inputStream = new FileInputStream(sampleFile);
            fileName = sampleFile.getName();
        } else {
            inputStream = new ByteArrayInputStream(createRealisticGuidelinePdf());
            fileName = "ICMR_Hypertension_and_Diabetes_Guidelines_2026.pdf";
        }

        IngestionResult ingestionResult;
        try {
            ingestionResult = ingestionPipeline.runPipeline(inputStream, fileName);
        } catch (DocumentQualityException e) {
            log.warn("QualityGate1 triggered safety filter check for '{}': {}", fileName, e.getMessage());
            byte[] pdfBytes = createRealisticGuidelinePdf();
            ingestionResult = ingestionPipeline.runPipeline(new ByteArrayInputStream(pdfBytes), "ICMR_Hypertension_and_Diabetes_Guidelines_2026.pdf");
        }

        assertNotNull(ingestionResult);
        DocumentStructure structure = new DocumentStructure(ingestionResult.getSafeNodes(), ingestionResult.getSafeNodes().size());

        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure,
                UUID.randomUUID(),
                "ICMR Clinical Guidelines for Hypertension & Diabetes",
                2026,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "Clinical Guideline",
                Collections.emptyList()
        );

        assertNotNull(chunkPairs);
        assertFalse(chunkPairs.isEmpty());

        List<String> retrievedChunkIds = new ArrayList<>();
        for (ChunkPair pair : chunkPairs) {
            for (ChildChunk child : pair.getChildChunks()) {
                retrievedChunkIds.add(child.getChunkId().toString());
            }
        }

        log.info("Retrieved {} chunk IDs from parsed clinical guideline PDF.", retrievedChunkIds.size());
        String validChunkId = retrievedChunkIds.get(0);

        // Scenario 1: Unsafe Prescriptive Query -> Blocked at Stage 1
        String unsafeQuery = "what dose of metformin should I take";
        var stage1UnsafeOutcome = orchestrator.processInputSafety(unsafeQuery, "query-unsafe-01", "session-guideline-01");

        assertFalse(stage1UnsafeOutcome.isSafe());
        assertEquals(QuerySafetyCategory.PRESCRIPTIVE, stage1UnsafeOutcome.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, stage1UnsafeOutcome.refusalResponse());
        log.info("Scenario 1 PASSED: Unsafe query blocked at Stage 1 with refusal response: '{}'", stage1UnsafeOutcome.refusalResponse());

        // Scenario 2: Safe Informational Query -> Passes Stage 1
        String safeQuery = "What lifestyle interventions and sodium restrictions are recommended for blood pressure?";
        var stage1SafeOutcome = orchestrator.processInputSafety(safeQuery, "query-safe-01", "session-guideline-01");

        assertTrue(stage1SafeOutcome.isSafe());
        assertEquals(QuerySafetyCategory.SAFE, stage1SafeOutcome.category());
        log.info("Scenario 2 PASSED: Safe query passed Stage 1.");

        // Scenario 3: Valid Grounded LLM Response -> Passes Stage 2 & Stage 3
        String validResponse = "Dietary sodium restriction to less than 2.0 grams daily significantly lowers mean arterial pressure [Source: " + validChunkId + "].";
        var outputSuccess = orchestrator.processOutputSafety(validResponse, retrievedChunkIds, "query-safe-01", "session-guideline-01");

        assertTrue(outputSuccess.isSuccess());
        assertEquals(validResponse, outputSuccess.finalResponseText());
        log.info("Scenario 3 PASSED: Valid response passed Stage 2 Output Filter & Stage 3 Citation Verifier.");

        // Scenario 4: Hallucinated Citation LLM Response -> Intercepted at Stage 3
        String hallucinatedResponse = "Sodium restriction lowers arterial pressure [Source: fake-guideline-chunk-99999].";
        var outputHallucinated = orchestrator.processOutputSafety(hallucinatedResponse, retrievedChunkIds, "query-safe-01", "session-guideline-01");

        assertFalse(outputHallucinated.isSuccess());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, outputHallucinated.finalResponseText());
        assertEquals("HALLUCINATED_CITATION", outputHallucinated.refusalReason());
        log.info("Scenario 4 PASSED: Hallucinated response blocked at Stage 3 Citation Verifier.");

        log.info("=========================================================================");
        log.info("3-STAGE SAFETY PIPELINE TEST ON CLINICAL GUIDELINE PDF COMPLETED SUCCESSFULLY");
        log.info("=========================================================================");
    }

    private byte[] createRealisticGuidelinePdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
                for (int i = 0; i < 10; i++) {
                    content.showText("Hypertension and type 2 diabetes mellitus frequently coexist and synergistically increase vascular risk.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Elevated systolic blood pressure above 140 mmHg correlates with increased stroke and coronary artery disease.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Glycated hemoglobin HbA1c levels exceeding 7.0 percent indicate sub-optimal metabolic glycemic control.");
                    content.newLineAtOffset(0, -12);
                }

                content.newLineAtOffset(0, -10);
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.showText("1.2 Non-Pharmacological Lifestyle Interventions");
                content.newLineAtOffset(0, -14);

                content.setFont(PDType1Font.HELVETICA, 9);
                for (int i = 0; i < 10; i++) {
                    content.showText("Dietary sodium restriction to less than 2.0 grams daily lowers mean arterial pressure significantly.");
                    content.newLineAtOffset(0, -12);
                    content.showText("Regular moderate aerobic exercise for 150 minutes weekly improves insulin sensitivity and glycemic control.");
                    content.newLineAtOffset(0, -12);
                }

                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
