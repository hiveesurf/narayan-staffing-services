package com.narayan.staffing.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long accessExpiresIn, UserProfile user) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 80) String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 80) String newPassword) {
    }

    public record UserProfile(UUID id, String email, String fullName, String phone, boolean active, Set<String> roles, Set<String> permissions, Instant lastLoginAt) {
    }

    public record UserRequest(
            @NotBlank @Email String email,
            @Size(min = 8, max = 80) String password,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 30) String phone,
            boolean active,
            @jakarta.validation.constraints.NotEmpty List<String> roles
    ) {
    }

    public record MessageResponse(String message) {
    }
}
