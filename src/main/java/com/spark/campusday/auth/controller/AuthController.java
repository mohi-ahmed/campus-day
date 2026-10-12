package com.spark.campusday.auth.controller;

import com.spark.campusday.auth.dto.AuthRequests;
import com.spark.campusday.auth.dto.AuthResponses;
import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.auth.service.AuthService;
import com.spark.campusday.auth.service.SessionTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SessionTokenService sessions;

    @PostMapping("/register")
    ResponseEntity<AuthResponses.RegistrationResponse> register(
            @Valid @RequestBody AuthRequests.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    AuthResponses.TokenResponse verifyEmail(
            @Valid @RequestBody AuthRequests.VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    ResponseEntity<AuthResponses.RegistrationResponse> resendVerification(
            @Valid @RequestBody AuthRequests.EmailRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(authService.resendVerification(request));
    }

    @PostMapping("/login")
    AuthResponses.TokenResponse login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    AuthResponses.TokenResponse refresh(
            @Valid @RequestBody AuthRequests.RefreshTokenRequest request) {
        return sessions.rotate(request.refreshToken());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody AuthRequests.RefreshTokenRequest request) {
        sessions.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    ResponseEntity<AuthResponses.MessageResponse> forgotPassword(
            @Valid @RequestBody AuthRequests.EmailRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(authService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    AuthResponses.MessageResponse resetPassword(
            @Valid @RequestBody AuthRequests.ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/oauth/exchange")
    AuthResponses.TokenResponse exchangeOAuthCode(
            @Valid @RequestBody AuthRequests.OAuthExchangeRequest request) {
        return authService.exchangeOAuthCode(request.code());
    }

    @PostMapping("/complete-profile")
    AuthResponses.TokenResponse completeProfile(
            @AuthenticationPrincipal CampusUserPrincipal principal,
            @Valid @RequestBody AuthRequests.CompleteProfileRequest request) {
        return authService.completeProfile(principal, request);
    }

    @GetMapping("/me")
    AuthResponses.AccountResponse me(@AuthenticationPrincipal CampusUserPrincipal principal) {
        return authService.currentAccount(principal);
    }
}
