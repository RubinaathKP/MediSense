package com.medisense.api;

import com.medisense.api.dto.AuthDtos.*;
import com.medisense.api.security.JwtTokenProvider;
import com.medisense.api.security.TokenBlacklistService;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private AuthController controller;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);

        controller = new AuthController(userRepository, passwordEncoder, jwtTokenProvider, tokenBlacklistService);
    }

    @Test
    void testRegisterSuccessHappyPath() {
        RegisterRequest request = new RegisterRequest("test@medisense.ai", "Password123!", "en");
        when(userRepository.findByEmail("test@medisense.ai")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_pass");

        UUID userId = UUID.randomUUID();
        UserEntity savedUser = UserEntity.builder().id(userId).email("test@medisense.ai").build();
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        ResponseEntity<RegisterResponse> response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().user_id());
    }

    @Test
    void testRegisterFailsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("existing@medisense.ai", "Password123!", "en");
        when(userRepository.findByEmail("existing@medisense.ai")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(IllegalArgumentException.class, () -> controller.register(request));
    }

    @Test
    void testLoginSuccessHappyPath() {
        LoginRequest request = new LoginRequest("user@medisense.ai", "CorrectPassword123!");
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("user@medisense.ai")
                .passwordHash("hashed_pass")
                .failedAttempts(0)
                .build();

        when(userRepository.findByEmail("user@medisense.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123!", "hashed_pass")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId, "user@medisense.ai")).thenReturn("access_token_123");
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refresh_token_123");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access_token_123", response.getBody().access_token());
        assertEquals("refresh_token_123", response.getBody().refresh_token());
    }

    @Test
    void testLoginFailsWithInvalidPasswordAndIncrementsFailedAttempts() {
        LoginRequest request = new LoginRequest("user@medisense.ai", "WrongPassword!");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@medisense.ai")
                .passwordHash("hashed_pass")
                .failedAttempts(2)
                .build();

        when(userRepository.findByEmail("user@medisense.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashed_pass")).thenReturn(false);

        assertThrows(SecurityException.class, () -> controller.login(request));
        verify(userRepository).save(argThat(u -> u.getFailedAttempts() == 3));
    }

    @Test
    void testLoginLocksAccountAfter5FailedAttempts() {
        LoginRequest request = new LoginRequest("user@medisense.ai", "WrongPassword!");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@medisense.ai")
                .passwordHash("hashed_pass")
                .failedAttempts(4)
                .build();

        when(userRepository.findByEmail("user@medisense.ai")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashed_pass")).thenReturn(false);

        assertThrows(SecurityException.class, () -> controller.login(request));
        verify(userRepository).save(argThat(u -> u.getFailedAttempts() == 5 && u.getLockedUntil() != null));
    }

    @Test
    void testLoginFailsWhenAccountIsCurrentlyLocked() {
        LoginRequest request = new LoginRequest("user@medisense.ai", "AnyPassword!");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@medisense.ai")
                .passwordHash("hashed_pass")
                .failedAttempts(5)
                .lockedUntil(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(userRepository.findByEmail("user@medisense.ai")).thenReturn(Optional.of(user));

        SecurityException ex = assertThrows(SecurityException.class, () -> controller.login(request));
        assertTrue(ex.getMessage().contains("Account locked"));
    }

    @Test
    void testLogoutBlacklistsToken() {
        String authHeader = "Bearer sample_jwt_token";
        java.util.Date exp = new java.util.Date(System.currentTimeMillis() + 600000);
        when(jwtTokenProvider.getExpirationFromToken("sample_jwt_token")).thenReturn(exp);

        ResponseEntity<LogoutResponse> response = controller.logout(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tokenBlacklistService).blacklistToken("sample_jwt_token", exp);
    }
}
