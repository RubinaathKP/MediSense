package com.medisense.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiRateLimiterFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimiterFilter.class);

    @Value("${medisense.ratelimit.requests-per-minute:20}")
    private int userRequestsPerMinute;

    @Value("${medisense.ratelimit.admin-requests-per-minute:10}")
    private int adminRequestsPerMinute;

    private final Map<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply rate limiting to API endpoints
        if (path == null || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        boolean isAdminPath = path.startsWith("/api/v1/admin");
        int maxAllowed = isAdminPath ? adminRequestsPerMinute : userRequestsPerMinute;

        String key = (isAdminPath ? "admin:" : "user:") + clientIp;
        long currentWindowMinute = System.currentTimeMillis() / 60000;

        RateLimitBucket bucket = rateLimitBuckets.compute(key, (k, existing) -> {
            if (existing == null || existing.windowMinute() != currentWindowMinute) {
                return new RateLimitBucket(currentWindowMinute, new AtomicInteger(1));
            }
            existing.requestCount().incrementAndGet();
            return existing;
        });

        if (bucket.requestCount().get() > maxAllowed) {
            log.warn("Rate limit exceeded for IP '{}' on path '{}'. Count: {}, Max: {}",
                    clientIp, path, bucket.requestCount().get(), maxAllowed);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded (%d req/min). Please try again in 60 seconds.\"}",
                    maxAllowed
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    public void resetRateLimitBuckets() {
        rateLimitBuckets.clear();
    }

    public record RateLimitBucket(long windowMinute, AtomicInteger requestCount) {}
}
