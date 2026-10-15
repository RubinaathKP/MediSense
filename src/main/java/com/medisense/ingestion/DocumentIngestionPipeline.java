package com.medisense.ingestion;

import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.ingestion.model.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DocumentIngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionPipeline.class);

    private final FormatParserService formatParserService;
    private final QualityGate1Service qualityGate1Service;
    private final TextCleanerService textCleanerService;
    private final StructureExtractorService structureExtractorService;
    private final PrescriptiveContentDetectorService prescriptiveContentDetectorService;

    public DocumentIngestionPipeline(FormatParserService formatParserService,
                                     QualityGate1Service qualityGate1Service,
                                     TextCleanerService textCleanerService,
                                     StructureExtractorService structureExtractorService,
                                     PrescriptiveContentDetectorService prescriptiveContentDetectorService) {
        this.formatParserService = formatParserService;
        this.qualityGate1Service = qualityGate1Service;
        this.textCleanerService = textCleanerService;
        this.structureExtractorService = structureExtractorService;
        this.prescriptiveContentDetectorService = prescriptiveContentDetectorService;
    }

    public IngestionResult runPipeline(InputStream pdfInputStream, String documentName) {
        log.info("==================================================================");
        log.info("Starting MediSense 5-Stage Ingestion Pipeline for '{}'", documentName);
        log.info("==================================================================");

        // Stage 1: FormatParser (PDFBox + Tika)
        ParsedDocument parsedDoc = formatParserService.parseFormat(pdfInputStream, documentName);

        // Stage 2: QualityGate1 (Confidence, Token Count, Formulary Regex, Rejection DB Queue)
        qualityGate1Service.validateDocument(parsedDoc);

        // Stage 3: TextCleaner (Headers/Footers, Hyphenation, Medical Abbreviations)
        CleanedDocument cleanedDoc = textCleanerService.cleanText(parsedDoc);

        // Stage 4: StructureExtractor (Chapter -> Section -> Subsection -> Paragraph Tree)
        DocumentStructure structure = structureExtractorService.extractStructure(cleanedDoc);

        // Stage 5: PrescriptiveContentDetector (Dosage, Protocol & Formulary Exclusions)
        IngestionResult result = prescriptiveContentDetectorService.detectAndFilter(structure, documentName);

        log.info("==================================================================");
        log.info("MediSense Ingestion Pipeline completed successfully for '{}'", documentName);
        log.info("Summary: Total Scanned={}, Excluded={}, Safe Nodes={}",
                result.getTotalNodesScanned(), result.getExcludedNodeCount(), result.getSafeNodes().size());
        log.info("==================================================================");

        return result;
    }

    public IngestionResult processDocument(InputStream pdfInputStream, String documentName) {
        return runPipeline(pdfInputStream, documentName);
    }
}
