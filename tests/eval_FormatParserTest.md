# Evaluation Report: eval_FormatParserTest

## 1. Purpose
The purpose of `FormatParserTest` is to validate that the document ingestion pipeline accurately parses multi-page medical lab reports and clinical formulary PDFs. It ensures layout-aware text extraction using Apache PDFBox and Apache Tika, strictly rejects non-text/image-only PDFs to prevent empty vector embeddings, and gracefully rejects corrupted files without memory leaks or unhandled exceptions.

## 2. Approach of Test
- **Isolation Strategy:** Pure unit test executing `FormatParserService` in an isolated JVM context.
- **Fixture Design:** Real synthetic PDFs generated via PDFBox matching clinical report layouts (CBC & Lipid Profile), tables, zero-text scanned pages, and corrupted byte streams.
- **Assertion Strategy:** Verifies exact diagnostic biomarker strings (`LDL Cholesterol`, `145 mg/dL`), extraction confidence metrics (threshold >= 0.85), token counts (>20 tokens), and targeted exception throws (`DocumentParsingException`).

## 3. Test Cases Matrix

| Test Case ID | Description | Input Fixture | Expected Behavior | Actual Behavior | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-FP-001** | Valid lab report text extraction | `valid_lab_report.pdf` (1.5 KB) | Extracted text contains clinical values, confidence >= 0.85, tokenCount > 20 | Parsed successfully; confidence 0.95; tokenCount 84 | **PASSED** |
| **TC-FP-002** | Rejection of image-only PDF | `invalid_lab_report_image_only.pdf` (624 B) | Throws `DocumentParsingException` on zero extracted text | Thrown `DocumentParsingException: Insufficient text extracted` | **PASSED** |
| **TC-FP-003** | Rejection of corrupted file | `corrupted.pdf` (70 B) | Throws `DocumentParsingException` on malformed header | Thrown `DocumentParsingException: PDFBox document parsing error` | **PASSED** |
| **TC-FP-004** | Tabular formulary parsing | `formulary_table.pdf` (1.3 KB) | Table structure preserved with drug classes | All drug rows extracted with correct columnar alignment | **PASSED** |

## 4. Test Execution Results
- **Overall Status:** :white_check_mark: **PASSED**
- **Test Score:** **100% (4 / 4 passed)**
- **Execution Time:** 112 ms
- **Memory Consumption:** ~18 MB
- **Coverage:** 94.2% line coverage on `FormatParserService`
- **Clinical Safety Assurance:** Ensures no silent ingestion of empty documents or unreadable scans into the medical knowledge base.
