package com.spark.campusday.auth.dto;

import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.StudentGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotNull AcademicYear academicYear,
            @NotNull StudentGender gender,
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9](?:[ -]?[0-9]){9,14}$", message = "must contain 10 to 15 digits")
            String phoneNumber,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record VerifyEmailRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otp
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record EmailRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otp,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record OAuthExchangeRequest(@NotBlank String code) {
    }

    public record CompleteProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotNull AcademicYear academicYear,
            @NotNull StudentGender gender,
            @NotBlank
            @Pattern(regexp = "^\\+?[0-9](?:[ -]?[0-9]){9,14}$", message = "must contain 10 to 15 digits")
            String phoneNumber
    ) {
    }
}
