package com.medisense.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenBlacklistService — Manages invalidated JWT tokens via Redis SET with TTL
 * matching remaining lifetime, with concurrent in-memory fallback.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final Set<String> localBlacklist = ConcurrentHashMap.newKeySet();

    public TokenBlacklistService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, Date expirationDate) {
        long remainingMs = expirationDate.getTime() - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return;
        }

        String hash = hashToken(token);
        String key = BLACKLIST_KEY_PREFIX + hash;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(remainingMs));
                log.info("Blacklisted JWT token in Redis with TTL {}ms until expiration: {}", remainingMs, expirationDate);
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for token blacklisting, falling back to local memory: {}", e.getMessage());
            }
        }
        localBlacklist.add(hash);
    }

    public void blacklistJti(String jti, Date expirationDate) {
        if (jti == null || jti.isBlank()) return;
        long remainingMs = expirationDate.getTime() - System.currentTimeMillis();
        if (remainingMs <= 0) return;

        String key = BLACKLIST_KEY_PREFIX + "jti:" + jti;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(remainingMs));
                log.info("Blacklisted JWT ID (jti) in Redis: {}", jti);
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for jti blacklisting, falling back to local memory: {}", e.getMessage());
            }
        }
        localBlacklist.add("jti:" + jti);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String hash = hashToken(token);
        if (localBlacklist.contains(hash)) {
            return true;
        }

        if (redisTemplate != null) {
            try {
                String key = BLACKLIST_KEY_PREFIX + hash;
                Boolean hasKey = redisTemplate.hasKey(key);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                log.debug("Redis unavailable for blacklist check: {}", e.getMessage());
            }
        }
        return false;
    }

    public boolean isJtiBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        if (localBlacklist.contains("jti:" + jti)) {
            return true;
        }
        if (redisTemplate != null) {
            try {
                String key = BLACKLIST_KEY_PREFIX + "jti:" + jti;
                Boolean hasKey = redisTemplate.hasKey(key);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                log.debug("Redis unavailable for jti check: {}", e.getMessage());
            }
        }
        return false;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return Integer.toHexString(token.hashCode());
        }
    }
}
