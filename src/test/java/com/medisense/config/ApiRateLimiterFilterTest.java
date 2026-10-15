package com.medisense.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiRateLimiterFilterTest {

    private ApiRateLimiterFilter rateLimiterFilter;

    @BeforeEach
    void setUp() {
        rateLimiterFilter = new ApiRateLimiterFilter();
        ReflectionTestUtils.setField(rateLimiterFilter, "userRequestsPerMinute", 3);
        ReflectionTestUtils.setField(rateLimiterFilter, "adminRequestsPerMinute", 2);
        rateLimiterFilter.resetRateLimitBuckets();
    }

    @Test
    @DisplayName("Test API Rate Limiting: User requests below limit pass through")
    void testUserRequestsBelowLimitPass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/reports/upload");
        request.setRemoteAddr("192.168.1.50");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        rateLimiterFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Test API Rate Limiting: Returns HTTP 429 Too Many Requests when rate limit is exceeded")
    void testRateLimitExceededReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/reports/upload");
        request.setRemoteAddr("192.168.1.100");

        FilterChain filterChain = mock(FilterChain.class);

        // Send 3 requests (allowed per limit)
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimiterFilter.doFilter(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        // 4th request exceeds limit
        MockHttpServletResponse responseExceeded = new MockHttpServletResponse();
        rateLimiterFilter.doFilter(request, responseExceeded, filterChain);

        assertEquals(429, responseExceeded.getStatus());
        assertTrue(responseExceeded.getContentAsString().contains("Rate limit exceeded"));
    }
}
