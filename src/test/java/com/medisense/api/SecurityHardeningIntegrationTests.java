package com.medisense.api;

import com.medisense.abdm.ConsentRequestService;
import com.medisense.api.dto.QueryDtos.QueryRequest;
import com.medisense.api.dto.ReportDtos.ReportUploadResponse;
import com.medisense.api.exception.InvalidFileTypeException;
import com.medisense.api.ratelimit.RateLimitConfig;
import com.medisense.api.security.JwtTokenProvider;
import com.medisense.api.security.TokenBlacklistService;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.ingestion.PdfParserService;
import com.medisense.ingestion.security.FileSecurityScanner;
import com.medisense.ingestion.security.NoOpFileSecurityScanner;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.translation.TranslationOrchestrator;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MediSense Security Hardening & Integration Test Suite")
class SecurityHardeningIntegrationTests {

    private ReportController reportController;
    private QueryController queryController;

    private PdfParserService pdfParserService;
    private ConsentRequestService consentRequestService;
    private SessionRepository sessionRepository;
    private FileSecurityScanner fileSecurityScanner;

    private InputSafetyClassifier safetyClassifier;
    private HybridRetrieverService hybridRetrieverService;
    private RagGenerationService ragGenerationService;
    private TranslationOrchestrator translationOrchestrator;

    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;

    private final UUID authenticatedUserId = UUID.randomUUID();
    private final UUID attackerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pdfParserService = mock(PdfParserService.class);
        consentRequestService = mock(ConsentRequestService.class);
        sessionRepository = mock(SessionRepository.class);
        fileSecurityScanner = new NoOpFileSecurityScanner();

        safetyClassifier = mock(InputSafetyClassifier.class);
        hybridRetrieverService = mock(HybridRetrieverService.class);
        ragGenerationService = mock(RagGenerationService.class);
        translationOrchestrator = mock(TranslationOrchestrator.class);

        jwtTokenProvider = new JwtTokenProvider("", "", 900000, 604800000);
        tokenBlacklistService = new TokenBlacklistService(null);

        reportController = new ReportController(
                pdfParserService,
                consentRequestService,
                sessionRepository,
                fileSecurityScanner
        );

        queryController = new QueryController(
                safetyClassifier,
                hybridRetrieverService,
                ragGenerationService,
                translationOrchestrator,
                sessionRepository
        );

        // Set authenticated user context
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                authenticatedUserId,
                "patient@medisense.in",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. Magic byte PDF validation
    @Test
    @DisplayName("Magic Byte Validation: Rejects uploaded file with spoofed .pdf extension but invalid header bytes")
    void testMagicBytePdfValidation() {
        byte[] fakePdfBytes = "THIS_IS_NOT_A_PDF_FILE_HEADER".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "malicious_report.pdf",
                "application/pdf",
                fakePdfBytes
        );

        InvalidFileTypeException ex = assertThrows(InvalidFileTypeException.class, () ->
                reportController.uploadReport(fakeFile, null, "lab_report")
        );

        assertTrue(ex.getMessage().contains("%PDF signature missing") || ex.getMessage().contains("file header"));
    }

    // 2. IDOR prevention
    @Test
    @DisplayName("IDOR Defense: Blocks user from accessing another user's session data")
    void testIdorPreventionOnSessionAccess() {
        UUID otherUserSessionId = UUID.randomUUID();
        UserEntity victimUser = UserEntity.builder()
                .id(attackerUserId) // Owned by different user
                .email("victim@medisense.in")
                .passwordHash("hashed")
                .build();

        SessionEntity victimSession = SessionEntity.builder()
                .id(otherUserSessionId)
                .user(victimUser)
                .sessionToken("token_123")
                .languageCode("en")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();

        when(sessionRepository.findById(otherUserSessionId)).thenReturn(Optional.of(victimSession));

        // Authenticated user is authenticatedUserId != victimUser.id
        assertThrows(AccessDeniedException.class, () ->
                reportController.getReportDetails(otherUserSessionId)
        );

        QueryRequest queryRequest = new QueryRequest(
                otherUserSessionId,
                "What does my LDL mean?",
                "lab_explanation",
                "en"
        );

        assertThrows(AccessDeniedException.class, () ->
                queryController.submitQuery(queryRequest)
        );
    }

    // 3. JWT blacklist
    @Test
    @DisplayName("JWT Blacklist: Blacklisting token prevents further authenticated usage")
    void testJwtBlacklistInvalidation() {
        String token = jwtTokenProvider.generateAccessToken(authenticatedUserId, "patient@medisense.in");
        assertTrue(jwtTokenProvider.validateToken(token));
        assertFalse(tokenBlacklistService.isBlacklisted(token));

        Date expiration = jwtTokenProvider.getExpirationFromToken(token);
        String jti = jwtTokenProvider.getJtiFromToken(token);
        assertNotNull(jti);

        // Blacklist token upon logout
        tokenBlacklistService.blacklistToken(token, expiration);
        tokenBlacklistService.blacklistJti(jti, expiration);

        assertTrue(tokenBlacklistService.isBlacklisted(token));
        assertTrue(tokenBlacklistService.isJtiBlacklisted(jti));
    }

    // 4. Rate limit across simulated multiple requests
    @Test
    @DisplayName("Rate Limiting: Enforces bucket token consumption and detects exhaustion")
    void testRateLimiterBucketExhaustion() {
        RateLimitConfig rateLimitConfig = new RateLimitConfig();
        Bucket queryBucket = rateLimitConfig.createQueryBucket(); // 30 requests per hour

        // Consume all available tokens in bucket
        for (int i = 0; i < 30; i++) {
            assertTrue(queryBucket.tryConsume(1), "Request " + (i + 1) + " should be permitted");
        }

        // 31st request must be rejected
        assertFalse(queryBucket.tryConsume(1), "Request 31 must be rejected by Rate Limiter");
        assertEquals(0, queryBucket.getAvailableTokens());
    }

    // 5. File deletion after parse
    @Test
    @DisplayName("File Sandbox Cleanup: Verifies temp file is cleaned up after parsing")
    void testFileDeletionAfterParse() throws Exception {
        byte[] validPdf = "%PDF-1.4 Content for lab report testing with valid PDF header".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "valid_report.pdf", "application/pdf", validPdf
        );

        when(pdfParserService.extractTextFromPdf(any(InputStream.class)))
                .thenReturn("Extracted text from valid report exceeding minimum threshold length 25 characters");

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        long tmpFilesBefore = countMedisenseTmpFiles(tmpDir);

        ResponseEntity<ReportUploadResponse> response = reportController.uploadReport(file, null, "lab_report");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        long tmpFilesAfter = countMedisenseTmpFiles(tmpDir);
        assertEquals(tmpFilesBefore, tmpFilesAfter, "Temporary upload files must be deleted in finally block");
    }

    private long countMedisenseTmpFiles(File dir) {
        File[] files = dir.listFiles((d, name) -> name.startsWith("medisense_upload_"));
        return files != null ? files.length : 0;
    }
}
