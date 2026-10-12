package com.spark.campusday.auth.dto;

import com.spark.campusday.auth.model.AuthProvider;

import java.time.Instant;
import java.util.Set;

public final class AuthResponses {
    private AuthResponses() {
    }

    public record MessageResponse(String message) {
    }

    public record RegistrationResponse(String email, Instant otpExpiresAt, String message) {
    }

    public record TokenResponse(
            String tokenType,
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            AccountResponse account
    ) {
    }

    public record AccountResponse(
            String id,
            String email,
            String studentId,
            boolean profileComplete,
            Set<AuthProvider> providers,
            Set<String> roles
    ) {
    }
}
