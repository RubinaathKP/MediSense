package com.medisense.translation;

import com.medisense.translation.model.TranslationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SarvamTranslationServiceTest {

    private NumericValueProtector valueProtector;
    private IntegrityVerifier integrityVerifier;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RestTemplate restTemplate;
    private SarvamTranslationService translationService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ProtectedTermsLoader termsLoader = new ProtectedTermsLoader();
        termsLoader.init();
        valueProtector = new NumericValueProtector(termsLoader, null);
        integrityVerifier = new IntegrityVerifier();
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        restTemplate = mock(RestTemplate.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        translationService = new SarvamTranslationService(
                valueProtector, integrityVerifier, redisTemplate, restTemplate);

        ReflectionTestUtils.setField(translationService, "apiKey", "test-sarvam-key");
        ReflectionTestUtils.setField(translationService, "baseUrl", "https://api.sarvam.ai");
    }

    @Test
    void testTranslateSuccessWithBucket4jAndRedisTracking() {
        String input = "Your LDL level is 145 mg/dL.";
        Map<String, String> sarvamResponseBody = Map.of("translated_text", "உங்களின் {TERM_1} அளவு {NUM_1} ஆகும்.");

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(sarvamResponseBody, HttpStatus.OK));

        TranslationResult result = translationService.translate(input, "ta");

        assertFalse(result.translationUnavailable());
        assertFalse(result.integrityCheckFailed());
        assertTrue(result.translatedText().contains("145 mg/dL"));
        verify(valueOperations).increment(startsWith("sarvam:rate:minute:"));
    }

    @Test
    void testTranslateRetryFallbackWhenSarvamApiFails() {
        String input = "Your LDL level is 145 mg/dL.";

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Sarvam API 503 Service Unavailable"));

        TranslationResult result = translationService.translate(input, "ta");

        assertTrue(result.translationUnavailable());
        assertEquals(input, result.translatedText());
        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(Map.class)); // 1 initial + 2 retries
    }
}
