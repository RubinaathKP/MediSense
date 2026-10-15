package com.medisense.unit.auth;

import com.medisense.UnitTestBase;
import com.medisense.api.AuthController;
import com.medisense.api.dto.AuthDtos.LoginRequest;
import com.medisense.api.dto.AuthDtos.AuthResponse;
import com.medisense.api.dto.AuthDtos.RegisterRequest;
import com.medisense.api.dto.AuthDtos.RegisterResponse;
import com.medisense.api.security.JwtTokenProvider;
import com.medisense.api.security.TokenBlacklistService;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit Test: AuthController
 * Verifies user registration, password hashing, duplicate email handling, and JWT authentication flows.
 */
class AuthControllerUnitTest extends UnitTestBase {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthController authController;

    @Override
    protected void onSetup() {
        authController = new AuthController(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                tokenBlacklistService
        );
    }

    @Test
    @DisplayName("Should successfully register a new user and return 201 Created")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest(
                "new.patient@example.com",
                "SecurePassword123!",
                "Rohan Verma",
                "en"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("$2a$10$hashedPasswordSample");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        ResponseEntity<RegisterResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("new.patient@example.com");
        assertThat(response.getBody().user_id()).isNotNull();
    }

    @Test
    @DisplayName("Should reject duplicate email registration with IllegalArgumentException")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "Password123!",
                "Existing User",
                "en"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(UserEntity.builder().email(request.email()).build()));

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Should authenticate valid credentials and issue JWT tokens")
    void shouldAuthenticateAndIssueTokens() {
        LoginRequest request = new LoginRequest("patient@example.com", "CorrectPassword123!");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .passwordHash("$2a$10$hashedPasswordSample")
                .failedAttempts(0)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().access_token()).isEqualTo("mock-access-token");
        assertThat(response.getBody().refresh_token()).isEqualTo("mock-refresh-token");
    }
}
