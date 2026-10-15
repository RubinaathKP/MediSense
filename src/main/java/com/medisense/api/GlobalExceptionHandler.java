package com.medisense.api;

import com.medisense.api.exception.*;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.safety.SafeRefusalResponse;
import com.medisense.safety.exception.HallucinatedCitationException;
import com.medisense.translation.TranslationIntegrityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error_code", "RATE_LIMIT_EXCEEDED");
        error.put("message", ex.getMessage());
        error.put("retry_after_seconds", ex.getRetryAfterSeconds());
        error.put("limit", ex.getLimit());
        error.put("window", ex.getWindow());
        error.put("referral_response", null);
        error.put("timestamp", OffsetDateTime.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", String.valueOf(ex.getLimit()));
        headers.set("X-RateLimit-Remaining", "0");
        headers.set("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + ex.getRetryAfterSeconds()));
        headers.set("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

        return new ResponseEntity<>(error, headers, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(DocumentQualityException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentQuality(DocumentQualityException ex) {
        return buildErrorResponse("DOCUMENT_QUALITY_FAILED", ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PrescriptiveContentException.class)
    public ResponseEntity<Map<String, Object>> handlePrescriptiveContent(PrescriptiveContentException ex) {
        return buildErrorResponse("PRESCRIPTIVE_CONTENT_DETECTED", ex.getMessage(), SafeRefusalResponse.DOCTOR_REFERRAL, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TranslationIntegrityException.class)
    public ResponseEntity<Map<String, Object>> handleTranslationIntegrity(TranslationIntegrityException ex) {
        // Translation integrity failure returns 200 with English fallback flag set
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error_code", "TRANSLATION_INTEGRITY_FAILED");
        response.put("message", "Translation verification failed. Falling back to original English text.");
        response.put("english_text", ex.getOriginalText());
        response.put("translation_unavailable", true);
        response.put("referral_response", null);
        response.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(HallucinatedCitationException.class)
    public ResponseEntity<Map<String, Object>> handleHallucinatedCitation(HallucinatedCitationException ex) {
        // Hallucinated citation returns 200 with safe referral response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error_code", "HALLUCINATED_CITATION_DETECTED");
        response.put("message", "Generated response failed citation verification.");
        response.put("referral_response", SafeRefusalResponse.DOCTOR_REFERRAL);
        response.put("is_referral", true);
        response.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(ABDMGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleAbdmGateway(ABDMGatewayException ex) {
        return buildErrorResponse("ABDM_GATEWAY_UNAVAILABLE", ex.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(LLMProviderException.class)
    public ResponseEntity<Map<String, Object>> handleLlmProvider(LLMProviderException ex) {
        return buildErrorResponse("LLM_PROVIDER_UNAVAILABLE", ex.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(ConsentExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleConsentExpired(ConsentExpiredException ex) {
        return buildErrorResponse("CONSENT_EXPIRED", ex.getMessage(), null, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotFound(SessionNotFoundException ex) {
        return buildErrorResponse("SESSION_NOT_FOUND", ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(FileTooLargeException ex) {
        return buildErrorResponse("FILE_TOO_LARGE", ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(InvalidFileTypeException ex) {
        return buildErrorResponse("INVALID_FILE_TYPE", ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error_code", "VALIDATION_FAILED");
        error.put("message", "Invalid request body fields: " + fieldErrors);
        error.put("field_errors", fieldErrors);
        error.put("referral_response", null);
        error.put("timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled Exception caught in API layer: {}", ex.getMessage(), ex);
        return buildErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred", null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String errorCode, String message, String referralResponse, HttpStatus status) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error_code", errorCode);
        error.put("message", message);
        error.put("referral_response", referralResponse);
        error.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(error);
    }
}
