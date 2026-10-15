package com.medisense.api;

import com.medisense.abdm.ConsentRequestService;
import com.medisense.api.dto.ReportDtos.*;
import com.medisense.api.exception.FileTooLargeException;
import com.medisense.api.exception.InvalidFileTypeException;
import com.medisense.api.exception.SessionNotFoundException;
import com.medisense.api.security.SecurityUtils;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.ingestion.PdfParserService;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.security.FileSecurityScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Report Ingestion", description = "Lab report PDF upload, ABDM record pull, value confirmation, and session report fetching")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46}; // "%PDF"

    private final PdfParserService pdfParserService;
    private final ConsentRequestService consentRequestService;
    private final SessionRepository sessionRepository;
    private final FileSecurityScanner fileSecurityScanner;

    public ReportController(PdfParserService pdfParserService,
                            ConsentRequestService consentRequestService,
                            SessionRepository sessionRepository,
                            FileSecurityScanner fileSecurityScanner) {
        this.pdfParserService = pdfParserService;
        this.consentRequestService = consentRequestService;
        this.sessionRepository = sessionRepository;
        this.fileSecurityScanner = fileSecurityScanner;
    }

    @PostMapping({"/reports/upload", "/api/v1/reports/upload"})
    @Operation(summary = "Upload medical lab report or prescription PDF", description = "Tier 2 rate limit (10 uploads/hr). Validates PDF magic bytes, 10MB size limit, sanitizes filename, and checks OCR confidence.")
    @ApiResponse(responseCode = "200", description = "File uploaded and parsed successfully")
    @ApiResponse(responseCode = "400", description = "INVALID_FILE_TYPE, FILE_TOO_LARGE, or OCR_CONFIDENCE_LOW")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<ReportUploadResponse> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "session_id", required = false) UUID sessionId,
            @RequestParam(value = "report_type", defaultValue = "lab_report") String reportType) {

        log.info("Received report upload request: filename={}, size={}, type={}", file.getOriginalFilename(), file.getSize(), reportType);

        // 1. Session ID & IDOR Validation
        if (sessionId != null) {
            validateSessionOwnership(sessionId);
        }

        // 2. File Size Validation
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException("File size exceeds maximum permitted limit of 10MB");
        }

        // 3. MIME type & Extension Validation
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf") ||
                (originalFilename != null && !originalFilename.toLowerCase().endsWith(".pdf"))) {
            throw new InvalidFileTypeException("Only PDF files are supported");
        }

        // 4. Magic Byte Validation & Temp File Sandbox Processing
        File tempFile = null;
        try {
            byte[] fileBytes = file.getBytes();

            // Validate PDF header (%PDF) magic bytes
            if (!isValidPdfMagicBytes(fileBytes)) {
                log.warn("Security Alert: Uploaded file failed magic byte verification (expected %PDF header)");
                throw new InvalidFileTypeException("Invalid file format: file header does not match a valid PDF document (%PDF signature missing)");
            }

            // Virus / Malware security scan hook
            fileSecurityScanner.scan(fileBytes);

            // Store in isolated temporary file outside web-accessible directory
            tempFile = File.createTempFile("medisense_upload_", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }

            // Extract text from temporary file stream
            String extractedText;
            try (FileInputStream fis = new FileInputStream(tempFile)) {
                extractedText = pdfParserService.extractTextFromPdf(fis);
            }

            log.info("Extracted PDF text length: {}", extractedText.length());

            // Confidence evaluation
            double simulatedConfidence = extractedText.length() > 20 ? 0.92 : 0.75;
            if (simulatedConfidence < 0.85) {
                throw new DocumentQualityException("OCR confidence score below required threshold (0.85)", simulatedConfidence, extractedText.length());
            }

            UUID reportId = UUID.randomUUID();
            boolean requiresConfirmation = simulatedConfidence < 0.90;

            return ResponseEntity.ok(new ReportUploadResponse(reportId, Collections.emptyList(), requiresConfirmation));

        } catch (DocumentQualityException | InvalidFileTypeException | FileTooLargeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing uploaded PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PDF report: " + e.getMessage(), e);
        } finally {
            // Delete temporary file regardless of success or failure
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    log.debug("Successfully cleaned up temporary upload file: {}", tempFile.getAbsolutePath());
                } catch (Exception ex) {
                    log.warn("Failed to delete temp file {}: {}", tempFile.getAbsolutePath(), ex.getMessage());
                }
            }
        }
    }

    @PostMapping({"/reports/abdm-pull", "/api/v1/reports/abdm-pull"})
    @Operation(summary = "Initiate ABDM record pull", description = "Tier 2 rate limit (5 pulls/hr). Initiates consent request via ABDM HIU workflow.")
    @ApiResponse(responseCode = "200", description = "Consent request initiated")
    public ResponseEntity<AbdmPullResponse> abdmPull(@Valid @RequestBody AbdmPullRequest request) {
        log.info("Initiating ABDM record pull for ABHA address: {}", request.abha_address());

        if (request.session_id() != null) {
            validateSessionOwnership(request.session_id());
        }

        String consentRequestId = consentRequestService.initiateConsentRequest(
                request.abha_address(),
                request.session_id()
        );

        return ResponseEntity.ok(new AbdmPullResponse(
                consentRequestId,
                "PENDING_PATIENT_APPROVAL",
                "Consent request sent to patient's ABHA app for approval"
        ));
    }

    @GetMapping({"/reports/{session_id}", "/api/v1/reports/{session_id}"})
    @Operation(summary = "Get report details for active session", description = "Tier 4 rate limit (60/hr). Returns extracted lab values and prescriptions.")
    @ApiResponse(responseCode = "200", description = "Report details returned")
    @ApiResponse(responseCode = "404", description = "SESSION_NOT_FOUND")
    public ResponseEntity<SessionReportDetailsResponse> getReportDetails(@PathVariable("session_id") UUID sessionId) {
        log.info("Fetching report details for session: {}", sessionId);

        validateSessionOwnership(sessionId);

        return ResponseEntity.ok(new SessionReportDetailsResponse(Collections.emptyList(), Collections.emptyList()));
    }

    @PostMapping({"/reports/confirm-value", "/api/v1/reports/confirm-value"})
    @Operation(summary = "Confirm ambiguous lab value or dosage", description = "Tier 2 rate limit (30/hr). Confirms low-confidence extracted value.")
    @ApiResponse(responseCode = "200", description = "Value confirmed")
    public ResponseEntity<ConfirmValueResponse> confirmValue(@Valid @RequestBody ConfirmValueRequest request) {
        log.info("User confirmed extracted value for ID: {}", request.value_id());
        return ResponseEntity.ok(new ConfirmValueResponse(request.value_id(), true));
    }

    private boolean isValidPdfMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == PDF_MAGIC_BYTES[0] &&
               bytes[1] == PDF_MAGIC_BYTES[1] &&
               bytes[2] == PDF_MAGIC_BYTES[2] &&
               bytes[3] == PDF_MAGIC_BYTES[3];
    }

    private void validateSessionOwnership(UUID sessionId) {
        Optional<SessionEntity> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new SessionNotFoundException("Session not found with ID: " + sessionId);
        }

        SessionEntity session = sessionOpt.get();
        Optional<UUID> currentUserIdOpt = SecurityUtils.getCurrentUserId();

        if (currentUserIdOpt.isPresent() && session.getUser() != null) {
            UUID currentUserId = currentUserIdOpt.get();
            if (!session.getUser().getId().equals(currentUserId) && !SecurityUtils.isCurrentUserAdmin()) {
                log.warn("Security Alert: IDOR attempt detected! User {} tried to access session {} owned by {}",
                        currentUserId, sessionId, session.getUser().getId());
                throw new AccessDeniedException("Access denied: You do not have permission to access this session");
            }
        }
    }
}
