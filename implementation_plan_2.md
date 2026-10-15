# Implementation Plan: MediSense Clinical Document Ingestion Pipeline

This document details the design and implementation strategy for the 5-stage document ingestion pipeline in `com.medisense.ingestion`.

## Pipeline Overview & Stage Responsibilities

```
Input PDF Document
       │
       ▼
┌───────────────────────────────┐
│ Stage 1: FormatParser        │ PDFBox + Apache Tika layout-aware extraction
└──────────────┬────────────────┘ Outputs: ParsedDocument
               │
               ▼
┌───────────────────────────────┐
│ Stage 2: QualityGate1         │ Confidence check, drug formulary detection, token count
└──────────────┬────────────────┘ Persists rejections to manual_review_queue table
               │ Throws DocumentQualityException on failure
               ▼
┌───────────────────────────────┐
│ Stage 3: TextCleaner          │ Header/footer removal, hyphenation normalization, abbreviation mapping
└──────────────┬────────────────┘ Outputs: CleanedDocument
               │
               ▼
┌───────────────────────────────┐
│ Stage 4: StructureExtractor   │ Hierarchy tagging (Chapter -> Section -> Subsection -> Paragraph)
└──────────────┬────────────────┘ Outputs: DocumentStructure & StructuredNode tree
               │
               ▼
┌───────────────────────────────┐
│ Stage 5: PrescriptiveDetector │ Scans for dosage + directive language & formulary patterns
└───────────────────────────────┘ Excludes prescriptive nodes with SLF4J audit logging
```

---

## 1. Domain Models & Exceptions (`com.medisense.ingestion.model` & `com.medisense.ingestion.exception`)

### Domain Models
- `ParsedDocument`: raw text, document structure tree, extracted tables as JSON (`List<String>`), extraction confidence (`double`)
- `CleanedDocument`: cleaned text, preserved section headings, cleaned paragraphs
- `StructuredNode`: level (`CHAPTER`, `SECTION`, `SUBSECTION`, `PARAGRAPH`), heading, text content, position path (e.g. `"1.2.1"`), child nodes
- `DocumentStructure`: root nodes tree, total node count
- `IngestionResult`: final output containing safe structured nodes, excluded nodes log, metadata

### Typed Exceptions
- `DocumentParsingException`: thrown during PDFBox/Tika parsing failures or image-only PDF detection
- `DocumentQualityException`: thrown when QualityGate1 criteria fail (confidence < 0.85, formulary pattern detected, tokens < 500)

---

## 2. Database Migration (`V2__add_manual_review_queue.sql`) & Entity

- Table `manual_review_queue`: `id`, `document_name`, `rejection_reason`, `rejection_stage`, `extraction_confidence`, `token_count`, `created_at`
- JPA Entity: `ManualReviewQueueEntity`
- JPA Repository: `ManualReviewQueueRepository`

---

## 3. Five Pipeline Stage Services

1. **`FormatParserService`**:
   - Uses PDFBox (`PDDocument`) & Apache Tika (`BodyContentHandler`, `AutoDetectParser`).
   - Detects image-only PDFs (no text layer / text token count == 0).
   - Computes layout-aware text & confidence score based on font/layout text ratio.
   - Extracts structured tables using Tika XML handler / layout parser.

2. **`QualityGate1Service`**:
   - Checks confidence >= 0.85.
   - Checks token count >= 500.
   - Runs drug formulary table regex matcher (`(?i)(drug|medicine|brand|generic)\s*.*(dosage|dose|frequency|route|administer)`).
   - If failed: logs rejection, persists `ManualReviewQueueEntity`, throws `DocumentQualityException`.

3. **`TextCleanerService`**:
   - Removes repeated header/footer lines across pages (e.g., page numbers `Page \d+ of \d+`).
   - Normalizes hyphens at line breaks (`word-\nbreak` -> `wordbreak`).
   - Normalizes whitespace.
   - Map-based medical abbreviation expander (e.g., `NCD` -> `Non-Communicable Disease`, `BP` -> `Blood Pressure`, `DM` -> `Diabetes Mellitus`).

4. **`StructureExtractorService`**:
   - Parses headings matching patterns (`Chapter \d+`, `\d+\.\d+`, `\d+\.\d+\.\d+`).
   - Builds hierarchical `StructuredNode` tree with position paths (`1`, `1.1`, `1.1.2`).

5. **`PrescriptiveContentDetectorService`**:
   - Scans text nodes against prescriptive regexes:
     - Dosage + directive: `\d+\s*(mg|ml|mcg|IU|units)` combined with `(?i)(take|administer|prescribe|give|dosage)`
     - Formulary pattern: `(?i)(drug|dose|frequency|route)`
     - Protocol pattern: `(?i)(first-line therapy|second-line treatment|dosing schedule|titrate)`
   - Filters out prescriptive nodes, logs exact matched pattern with WARN level SLF4J, returns safe node list.

---

## 4. Pipeline Coordinator Service (`DocumentIngestionPipeline`)

- Orchestrates Stage 1 → Stage 2 → Stage 3 → Stage 4 → Stage 5 sequentially.

---

## 5. Comprehensive Unit Test Suite (`src/test/java/com/medisense/ingestion`)

- `FormatParserServiceTest`: PDF parsing, image-only PDF detection, table extraction.
- `QualityGate1ServiceTest`: Low confidence rejection, drug formulary rejection, low token rejection, manual review persistence.
- `TextCleanerServiceTest`: Header/footer removal, hyphenation fix, abbreviation expansion.
- `StructureExtractorServiceTest`: Hierarchical tree construction, position path tagging.
- `PrescriptiveContentDetectorServiceTest`: Prescriptive text detection, dosage pattern exclusion, audit logging.
- `DocumentIngestionPipelineTest`: Full end-to-end pipeline execution and rejection routing.

---

## Verification Plan

### Automated Tests
- Run `mvn test` to verify all pipeline stage unit tests pass cleanly.
