package com.medisense.api;

import com.medisense.api.dto.AuthDtos.*;
import com.medisense.api.security.JwtTokenProvider;
import com.medisense.api.security.TokenBlacklistService;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Authentication", description = "User registration, login, token refresh, and logout endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping({"/auth/register", "/api/v1/auth/register"})
    @Operation(summary = "Register new user", description = "Tier 1 rate limit (5/hr/IP). Validates email, min 8 char password, and language preference.")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or email already exists")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        UserEntity user = UserEntity.builder()
                .email(request.email())
                .fullName(request.fullName() != null ? request.fullName() : "User")
                .passwordHash(passwordHash)
                .languagePreference(request.language_pref() != null ? request.language_pref() : "en")
                .failedAttempts(0)
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(savedUser.getId(), "User registered successfully", savedUser.getEmail()));
    }

    @PostMapping({"/auth/login", "/api/v1/auth/login"})
    @Operation(summary = "User login", description = "Tier 1 rate limit (10/hr/IP). Locks account after 5 consecutive failed attempts.")
    @ApiResponse(responseCode = "200", description = "Login successful, returns access & refresh tokens")
    @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Processing login request for user: {}", request.email());

        Optional<UserEntity> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            throw new SecurityException("Invalid email or password");
        }

        UserEntity user = userOpt.get();

        // Account Lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new SecurityException("Account locked due to 5 consecutive failed attempts. Please try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int newFailedCount = (user.getFailedAttempts() != null ? user.getFailedAttempts() : 0) + 1;
            user.setFailedAttempts(newFailedCount);
            if (newFailedCount >= 5) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
                log.warn("Account locked for 15 minutes due to 5 failed attempts: {}", user.getEmail());
            }
            userRepository.save(user);
            throw new SecurityException("Invalid email or password");
        }

        // Reset failed attempts on successful login
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, expiresIn, user.getId()));
    }

    @PostMapping({"/auth/refresh", "/api/v1/auth/refresh"})
    @Operation(summary = "Refresh JWT access token", description = "Tier 1 rate limit (30/hr/user). Generates new access token from valid refresh token.")
    @ApiResponse(responseCode = "200", description = "Access token refreshed")
    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.refresh_token();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new SecurityException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

        return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken, expiresIn, user.getId()));
    }

    @PostMapping({"/auth/logout", "/api/v1/auth/logout"})
    @Operation(summary = "User logout", description = "Blacklists current Bearer JWT token in Redis until token expiration.")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);
            tokenBlacklistService.blacklistToken(token, expiration);
        }
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }
}
