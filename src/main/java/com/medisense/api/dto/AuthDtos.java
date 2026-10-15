package com.medisense.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            String fullName,

            @Pattern(regexp = "^(en|ta)$", message = "Language preference must be 'en' or 'ta'")
            String language_pref
    ) {
        public RegisterRequest(String email, String password, String language_pref) {
            this(email, password, null, language_pref);
        }
    }

    public record RegisterResponse(
            UUID user_id,
            String message,
            String email
    ) {
        public RegisterResponse(UUID user_id, String message) {
            this(user_id, message, null);
        }
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record LoginResponse(
            String access_token,
            String refresh_token,
            long expires_in,
            UUID user_id
    ) {}

    public record AuthResponse(
            String access_token,
            String refresh_token,
            long expires_in,
            UUID user_id
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refresh_token
    ) {}

    public record LogoutResponse(
            String message
    ) {}
}
