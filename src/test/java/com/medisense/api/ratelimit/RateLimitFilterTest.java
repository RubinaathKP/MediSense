package com.medisense.api.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private RateLimitConfig config;
    private RateLimitService service;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        config = new RateLimitConfig();
        service = mock(RateLimitService.class);
        filterChain = mock(FilterChain.class);
        filter = new RateLimitFilter(config, service);
    }

    @Test
    void testFilterAllowsRequestWhenTokenConsumed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(service.tryConsume(any(), any()))
                .thenReturn(new RateLimitService.ConsumptionResult(true, 9, 3600, 10));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("10", response.getHeader("X-RateLimit-Limit"));
        assertEquals("9", response.getHeader("X-RateLimit-Remaining"));
        assertNotNull(response.getHeader("X-RateLimit-Reset"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testFilterReturns429WhenRateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/register");
        request.setRemoteAddr("192.168.1.101");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(service.tryConsume(any(), any()))
                .thenReturn(new RateLimitService.ConsumptionResult(false, 0, 120, 5));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("120", response.getHeader("Retry-After"));

        String body = response.getContentAsString();
        assertTrue(body.contains("RATE_LIMIT_EXCEEDED"));
        assertTrue(body.contains("retry_after_seconds"));
        verify(filterChain, never()).doFilter(request, response);
    }
}
