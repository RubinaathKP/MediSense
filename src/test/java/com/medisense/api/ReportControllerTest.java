package com.medisense.api;

import com.medisense.abdm.ConsentRequestService;
import com.medisense.api.dto.ReportDtos.*;
import com.medisense.api.exception.FileTooLargeException;
import com.medisense.api.exception.InvalidFileTypeException;
import com.medisense.api.exception.SessionNotFoundException;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.ingestion.PdfParserService;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.security.FileSecurityScanner;
import com.medisense.ingestion.security.NoOpFileSecurityScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private ReportController controller;
    private PdfParserService pdfParserService;
    private ConsentRequestService consentRequestService;
    private SessionRepository sessionRepository;
    private FileSecurityScanner fileSecurityScanner;

    @BeforeEach
    void setUp() {
        pdfParserService = mock(PdfParserService.class);
        consentRequestService = mock(ConsentRequestService.class);
        sessionRepository = mock(SessionRepository.class);
        fileSecurityScanner = new NoOpFileSecurityScanner();

        controller = new ReportController(pdfParserService, consentRequestService, sessionRepository, fileSecurityScanner);
    }

    @Test
    void testUploadReportSuccessHappyPath() throws Exception {
        byte[] pdfContent = "%PDF-1.4 Sample PDF content for lab report extraction test LDL 145 mg/dL".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "lab_report.pdf", "application/pdf", pdfContent
        );

        when(pdfParserService.extractTextFromPdf(any(InputStream.class)))
                .thenReturn("Sample PDF content for lab report extraction test LDL 145 mg/dL");

        ResponseEntity<ReportUploadResponse> response = controller.uploadReport(file, null, "lab_report");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().report_id());
    }

    @Test
    void testUploadReportRejectsNonPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "PNG binary content".getBytes()
        );

        assertThrows(InvalidFileTypeException.class, () -> controller.uploadReport(file, null, "lab_report"));
    }

    @Test
    void testUploadReportRejectsLargeFileExceeding10MB() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent
        );

        assertThrows(FileTooLargeException.class, () -> controller.uploadReport(file, null, "lab_report"));
    }

    @Test
    void testUploadReportThrowsDocumentQualityExceptionWhenConfidenceLow() throws Exception {
        byte[] shortPdfContent = "%PDF-short".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "blurry.pdf", "application/pdf", shortPdfContent
        );

        when(pdfParserService.extractTextFromPdf(any(InputStream.class))).thenReturn("short");

        assertThrows(DocumentQualityException.class, () -> controller.uploadReport(file, null, "lab_report"));
    }

    @Test
    void testAbdmPullSuccess() {
        AbdmPullRequest request = new AbdmPullRequest(null, "patient@sbx", List.of("DiagnosticReport"), "2026-01-01", "2026-08-01");
        when(consentRequestService.initiateConsentRequest(any(), any())).thenReturn("consent-req-789");

        ResponseEntity<AbdmPullResponse> response = controller.abdmPull(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("consent-req-789", response.getBody().consent_request_id());
        assertEquals("PENDING_PATIENT_APPROVAL", response.getBody().status());
    }

    @Test
    void testGetReportDetailsThrowsSessionNotFoundException() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> controller.getReportDetails(sessionId));
    }

    @Test
    void testConfirmValueSuccess() {
        UUID valueId = UUID.randomUUID();
        ConfirmValueRequest request = new ConfirmValueRequest(valueId, "145", "mg/dL");

        ResponseEntity<ConfirmValueResponse> response = controller.confirmValue(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().confirmed());
        assertEquals(valueId, response.getBody().value_id());
    }
}
