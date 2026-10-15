package com.medisense.translation.indictrans2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.translation.indictrans2.model.IndicTrans2Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IndicTrans2ClientTest {

    private IndicTrans2CircuitBreaker circuitBreaker;
    private ObjectMapper objectMapper;
    private IndicTrans2Client.HttpSender httpSender;
    private IndicTrans2Client client;

    @BeforeEach
    void setUp() {
        circuitBreaker = new IndicTrans2CircuitBreaker();
        objectMapper = new ObjectMapper();
        httpSender = Mockito.mock(IndicTrans2Client.HttpSender.class);

        client = new IndicTrans2Client(
                "http://indictrans2-service:8002/translate",
                circuitBreaker,
                objectMapper,
                httpSender
        );
    }

    @Test
    void testTranslateSuccessWithHttp200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"translated_text\":\"உங்களின் {NUM_1} அளவு\",\"src_lang\":\"eng_Latn\",\"tgt_lang\":\"tam_Tam\"}");

        when(httpSender.send(any())).thenReturn(response);

        IndicTrans2Response result = client.translate("{NUM_1}", "eng_Latn", "tam_Tam");

        assertNotNull(result);
        assertEquals("உங்களின் {NUM_1} அளவு", result.translated_text());
        assertEquals("CLOSED", circuitBreaker.getState());
    }

    @Test
    void testTranslateCircuitBreakerOpensAfterThreeFailures() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("Internal Server Error");
        when(httpSender.send(any())).thenReturn(response);

        // 3 consecutive failed executions (each attempt runs 3 times = 9 HTTP calls)
        assertThrows(IndicTrans2UnavailableException.class, () -> client.translate("test1", "eng_Latn", "tam_Tam"));
        assertThrows(IndicTrans2UnavailableException.class, () -> client.translate("test2", "eng_Latn", "tam_Tam"));
        assertThrows(IndicTrans2UnavailableException.class, () -> client.translate("test3", "eng_Latn", "tam_Tam"));

        assertEquals("OPEN", circuitBreaker.getState());

        // Subsequent call fails fast due to OPEN circuit breaker
        IndicTrans2UnavailableException ex = assertThrows(
                IndicTrans2UnavailableException.class,
                () -> client.translate("test4", "eng_Latn", "tam_Tam")
        );
        assertTrue(ex.getMessage().contains("Circuit breaker is OPEN"));
    }
}
