# Test Specification: ReportControllerTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.api.ReportController`
- **Category:** Unit Test (File Upload & Security Gate)
- **Framework:** JUnit 5, Mockito, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/api/ReportControllerUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.api;

import com.medisense.UnitTestBase;
import com.medisense.abdm.ConsentRequestService;
import com.medisense.api.ReportController;
import com.medisense.api.dto.ReportDtos.ReportUploadResponse;
import com.medisense.api.exception.FileTooLargeException;
import com.medisense.api.exception.InvalidFileTypeException;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.ingestion.PdfParserService;
import com.medisense.ingestion.model.ParsedDocument;
import com.medisense.ingestion.security.FileSecurityScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReportControllerUnitTest extends UnitTestBase {

    @Mock
    private PdfParserService pdfParserService;

    @Mock
    private ConsentRequestService consentRequestService;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private FileSecurityScanner fileSecurityScanner;

    private ReportController reportController;

    @Override
    protected void onSetup() {
        reportController = new ReportController(
                pdfParserService,
                consentRequestService,
                sessionRepository,
                fileSecurityScanner
        );
    }

    @Test
    @DisplayName("Should reject files exceeding 10MB with FileTooLargeException")
    void shouldRejectOversizedFiles() {
        byte[] oversizedBytes = new byte[11 * 1024 * 1024]; // 11MB
        oversizedBytes[0] = 0x25;
        oversizedBytes[1] = 0x50;
        oversizedBytes[2] = 0x44;
        oversizedBytes[3] = 0x46; // %PDF magic bytes

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", oversizedBytes
        );

        assertThatThrownBy(() -> reportController.uploadReport(file, UUID.randomUUID(), "LAB_REPORT"))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    @DisplayName("Should reject non-PDF file formats with InvalidFileTypeException")
    void shouldRejectNonPdfFiles() {
        byte[] textBytes = "This is plain text, not a PDF document.".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.txt", "text/plain", textBytes
        );

        assertThatThrownBy(() -> reportController.uploadReport(file, UUID.randomUUID(), "LAB_REPORT"))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    @DisplayName("Should accept valid PDF report and return parsed parameters")
    void shouldAcceptValidPdfReport() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(SessionEntity.builder().id(sessionId).build()));

        byte[] validPdfBytes = "%PDF-1.4 sample content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood_report.pdf", "application/pdf", validPdfBytes
        );

        ParsedDocument parsedDoc = ParsedDocument.builder()
                .documentName("blood_report.pdf")
                .rawText("Hemoglobin 14.2 g/dL")
                .extractionConfidence(0.95)
                .tokenCount(10)
                .metadata(Collections.emptyMap())
                .build();

        when(pdfParserService.parse(any(InputStream.class), eq("blood_report.pdf")))
                .thenReturn(parsedDoc);

        ResponseEntity<ReportUploadResponse> response = reportController.uploadReport(file, sessionId, "LAB_REPORT");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().document_name()).isEqualTo("blood_report.pdf");
        assertThat(response.getBody().extraction_confidence()).isEqualTo(0.95);
    }
}
```
