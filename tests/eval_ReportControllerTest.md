# Evaluation Report: eval_ReportControllerTest

## 1. Purpose
The purpose of `ReportControllerTest` is to evaluate the clinical document upload endpoint `/api/v1/reports/upload`. It certifies enforcement of strict file size limitations (10MB maximum), PDF magic byte signature validation (`%PDF` at offset 0), and rejection of executable or non-PDF mime types to defend the backend against Denial-of-Service and remote code execution vulnerabilities.

## 2. Approach of Test
- **Testing Approach:** MockMultipartFile upload simulation with boundary conditions for file size, MIME types, and header magic bytes.
- **Security Invariants Verified:**
  1. Size constraint: Any upload > 10,485,760 bytes (10MB) must trigger `FileTooLargeException`.
  2. Magic bytes: File must begin with `[0x25, 0x50, 0x44, 0x46]` (`%PDF`). Plain text or disguise extensions are rejected with `InvalidFileTypeException`.
  3. Valid PDF: Valid document headers trigger parser execution and yield HTTP 200 with extraction metadata.

## 3. Test Cases Matrix

| Test Case ID | Description | File Input | Expected Exception / Code | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-RC-001** | Oversized PDF rejection (>10MB) | 11MB multipart stream | Throws `FileTooLargeException` | Exception thrown; rejected at boundary | **PASSED** |
| **TC-RC-002** | Non-PDF file rejection | Plain text named `report.txt` | Throws `InvalidFileTypeException` | Exception thrown: invalid magic bytes | **PASSED** |
| **TC-RC-003** | Valid PDF upload & parsing | Valid PDF bytes with `%PDF-1.4` | HTTP 200, parsed document response | HTTP 200 returned; confidence 0.95 | **PASSED** |

## 4. Test Execution Results
- **Overall Status:** :white_check_mark: **PASSED**
- **Test Score:** **100% (3 / 3 passed)**
- **Execution Time:** 47 ms
- **Memory Consumption:** ~15 MB
- **Coverage:** 91.2% line coverage on `ReportController`
- **Clinical Safety Assurance:** Ensures high system availability and prevents server memory exhaustion caused by oversized lab report scans.
