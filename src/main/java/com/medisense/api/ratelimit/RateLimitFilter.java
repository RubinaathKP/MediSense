package com.medisense.api.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitConfig rateLimitConfig, RateLimitService rateLimitService) {
        this.rateLimitConfig = rateLimitConfig;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        Optional<RateLimitConfig.RateLimitRule> ruleOpt = rateLimitConfig.findRule(method, uri);

        if (ruleOpt.isPresent()) {
            RateLimitConfig.RateLimitRule rule = ruleOpt.get();
            String identifier = extractIdentifier(request, rule);

            RateLimitService.ConsumptionResult result = rateLimitService.tryConsume(rule, identifier);

            long resetEpochSeconds = (System.currentTimeMillis() / 1000) + result.secondsToRefill();

            if (result.isConsumed()) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(result.capacity()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
                response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));
            } else {
                response.setStatus(429);
                response.setContentType("application/json");
                response.setHeader("X-RateLimit-Limit", String.valueOf(result.capacity()));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));
                response.setHeader("Retry-After", String.valueOf(result.secondsToRefill()));

                String jsonResponse = String.format(
                        "{\"error_code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please try again later.\",\"retry_after_seconds\":%d,\"limit\":%d,\"window\":\"1 hour\"}",
                        result.secondsToRefill(),
                        result.capacity()
                );
                response.getWriter().write(jsonResponse);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractIdentifier(HttpServletRequest request, RateLimitConfig.RateLimitRule rule) {
        if (rule.byIp()) {
            return extractIpAddress(request);
        }

        if (rule.byAdminKey()) {
            String adminKey = request.getHeader("X-Admin-Key");
            return adminKey != null ? adminKey : extractIpAddress(request);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }

        return extractIpAddress(request);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
