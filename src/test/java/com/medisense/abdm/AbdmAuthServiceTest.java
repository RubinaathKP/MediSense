package com.medisense.abdm;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AbdmAuthServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RestTemplate restTemplate;
    private AbdmAuthService authService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        restTemplate = mock(RestTemplate.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService = new AbdmAuthService(redisTemplate, restTemplate);
        ReflectionTestUtils.setField(authService, "gatewayUrl", "https://dev.abdm.gov.in/gateway");
        ReflectionTestUtils.setField(authService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(authService, "clientSecret", "test-client-secret");
    }

    @Test
    void testGetValidAccessTokenFromRedisCache() {
        when(valueOperations.get("abdm:gateway:access_token")).thenReturn("cached_redis_token_123");

        String token = authService.getValidAccessToken();
        assertEquals("cached_redis_token_123", token);
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void testGetValidAccessTokenFetchesNewTokenWhenCacheEmpty() {
        when(valueOperations.get("abdm:gateway:access_token")).thenReturn(null);

        ResponseEntity<Map> response = new ResponseEntity<>(Map.of("accessToken", "new_gateway_token_456"), HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(response);

        String token = authService.getValidAccessToken();
        assertEquals("new_gateway_token_456", token);
        verify(valueOperations).set(eq("abdm:gateway:access_token"), eq("new_gateway_token_456"), eq(25L), any());
    }

    @Test
    void testGetValidAccessTokenFallbackWhenGatewayFails() {
        when(valueOperations.get("abdm:gateway:access_token")).thenReturn(null);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("ABDM Gateway connection timeout"));

        String token = authService.getValidAccessToken();
        assertNotNull(token);
        assertTrue(token.contains("sandbox_fallback_token_"));
    }
}
