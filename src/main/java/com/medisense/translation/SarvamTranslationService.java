package com.medisense.translation;

import com.medisense.translation.model.TranslationResult;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class SarvamTranslationService {

    private static final Logger log = LoggerFactory.getLogger(SarvamTranslationService.class);
    private static final int SARVAM_RATE_LIMIT_PER_MIN = 60;
    private static final int ALERT_THRESHOLD_PER_MIN = 48; // 80% of 60

    @Value("${medisense.translation.sarvam.api-key:mock-sarvam-key}")
    private String apiKey;

    @Value("${medisense.translation.sarvam.base-url:https://api.sarvam.ai}")
    private String baseUrl;

    private final NumericValueProtector valueProtector;
    private final IntegrityVerifier integrityVerifier;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    // Bucket4j Rate Limiter (60 tokens capacity, refills 60 tokens per minute)
    private final Bucket rateLimiterBucket;

    public SarvamTranslationService(NumericValueProtector valueProtector,
                                    IntegrityVerifier integrityVerifier,
                                    StringRedisTemplate redisTemplate,
                                    RestTemplate restTemplate) {
        this.valueProtector = valueProtector;
        this.integrityVerifier = integrityVerifier;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;

        Bandwidth limit = Bandwidth.builder()
                .capacity(SARVAM_RATE_LIMIT_PER_MIN)
                .refillIntervally(SARVAM_RATE_LIMIT_PER_MIN, Duration.ofMinutes(1))
                .build();
        this.rateLimiterBucket = Bucket.builder().addLimit(limit).build();
    }

    public TranslationResult translate(String englishText, String targetLang) {
        if (englishText == null || englishText.isBlank()) {
            return TranslationResult.success(englishText, englishText, "en", targetLang);
        }

        if (!"ta".equalsIgnoreCase(targetLang) && !"ta-IN".equalsIgnoreCase(targetLang)) {
            log.info("Target language '{}' requested without translation engine. Returning English.", targetLang);
            return TranslationResult.success(englishText, englishText, "en", targetLang);
        }

        // 1. Bucket4j Token Bucket Rate Limiting (Queue wait max 5 seconds)
        try {
            boolean acquired = rateLimiterBucket.asBlocking().tryConsume(1, Duration.ofSeconds(5));
            if (!acquired) {
                log.warn("Sarvam API Bucket4j rate limit queue wait exceeded 5 seconds. Falling back to English.");
                return TranslationResult.fallbackUnavailable(englishText, "Bucket4j rate limit queue wait timeout (>5s)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Bucket4j rate limiter wait interrupted.");
            return TranslationResult.fallbackUnavailable(englishText, "Rate limit wait interrupted");
        }

        // 2. Track usage in Redis
        trackRedisMinuteUsage();

        // 3. Numeric & Term Protection
        NumericValueProtector.ProtectionResult protection = valueProtector.protectValues(englishText);

        // 4. API Execution with Retry Strategy (2 retries with 1s, 2s backoff)
        String rawTranslatedOutput = executeSarvamApiWithRetry(protection.maskedText());
        if (rawTranslatedOutput == null) {
            log.error("Sarvam API translation failed after 2 retries. Returning English fallback.");
            return TranslationResult.fallbackUnavailable(englishText, "Sarvam API request failed after 2 retries.");
        }

        // 5. Placeholder Restoration
        String restoredText;
        try {
            restoredText = valueProtector.restoreValues(rawTranslatedOutput, englishText, protection.placeholderRegistry());
        } catch (TranslationIntegrityException e) {
            log.error("Placeholder restoration failed: {}", e.getMessage());
            return TranslationResult.fallbackIntegrityFailed(englishText, "Missing placeholder during restoration: " + e.getMissingPlaceholder());
        }

        // 6. Integrity Verification
        IntegrityVerifier.IntegrityCheckResult integrityCheck = integrityVerifier.verifyIntegrity(englishText, restoredText, protection.placeholderRegistry());
        if (!integrityCheck.passed()) {
            log.warn("Integrity verification failed for Sarvam translation: {}", integrityCheck.failureReason());
            return TranslationResult.fallbackIntegrityFailed(englishText, integrityCheck.failureReason());
        }

        log.info("Sarvam AI translation completed successfully with full numeric and drug name integrity.");
        return TranslationResult.success(restoredText, englishText, "en", "ta");
    }

    public String translateToTamil(String englishText) {
        TranslationResult result = translate(englishText, "ta");
        return result.translatedText();
    }

    private String executeSarvamApiWithRetry(String maskedText) {
        String url = baseUrl.endsWith("/") ? baseUrl + "translate" : baseUrl + "/translate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("api-key", apiKey);
        }

        Map<String, Object> requestBody = Map.of(
                "input", maskedText,
                "source_language_code", "en-IN",
                "target_language_code", "ta-IN",
                "speaker_gender", "Female",
                "mode", "formal",
                "model", "mayura:v1",
                "enable_preprocessing", true
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxAttempts = 3; // 1 initial + 2 retries
        long[] backoffDelaysMs = {1000, 2000};

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Calling Sarvam-Translate API (attempt {}/{}): url={}", attempt, maxAttempts, url);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map body = response.getBody();
                    if (body.containsKey("translated_text")) {
                        return (String) body.get("translated_text");
                    }
                }
            } catch (Exception e) {
                log.warn("Sarvam API attempt {} failed: {}", attempt, e.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffDelaysMs[attempt - 1]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Mock Fallback for local development/sandbox when API key is unconfigured
        if ("mock-sarvam-key".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.info("Generating mock Sarvam Tamil translation output for testing.");
            return mockTranslateMaskedText(maskedText);
        }

        return null;
    }

    private String mockTranslateMaskedText(String maskedText) {
        return maskedText + " (தமிழில் விளக்கம்)";
    }

    private void trackRedisMinuteUsage() {
        if (redisTemplate == null) return;
        try {
            long epochMinute = Instant.now().getEpochSecond() / 60;
            String redisKey = "sarvam:rate:minute:" + epochMinute;

            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(90));
            }

            if (count != null && count >= ALERT_THRESHOLD_PER_MIN) {
                log.warn("Sarvam API minute usage ALERT! Minute counter reached {} requests (>= 80% of limit {}).",
                        count, SARVAM_RATE_LIMIT_PER_MIN);
            }
        } catch (Exception e) {
            log.debug("Redis rate tracking failed: {}", e.getMessage());
        }
    }
}
