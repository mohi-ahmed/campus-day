package com.spark.campusday.auth.service;

import com.spark.campusday.auth.config.AuthProperties;
import com.spark.campusday.auth.dto.AuthRequests;
import com.spark.campusday.auth.dto.AuthResponses;
import com.spark.campusday.auth.model.AuthAccount;
import com.spark.campusday.auth.model.EmailOtpChallenge;
import com.spark.campusday.auth.model.OtpPurpose;
import com.spark.campusday.auth.model.PendingRegistration;
import com.spark.campusday.auth.repository.AuthAccountRepository;
import com.spark.campusday.auth.repository.PendingRegistrationRepository;
import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthAccountRepository accounts;
    private final PendingRegistrationRepository pendingRegistrations;
    private final StudentService studentService;
    private final OtpChallengeService otpChallenges;
    private final SessionTokenService sessions;
    private final OAuthLoginTicketService oauthTickets;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthProperties properties;

    public AuthResponses.RegistrationResponse register(AuthRequests.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phoneNumber());
        if (accounts.findByEmail(email).isPresent() || studentService.existsByEmail(email)) {
            throw new ConflictException("An account already exists for this email");
        }
        if (studentService.existsByPhoneNumber(phone)) {
            throw new ConflictException("An account already exists for this phone number");
        }

        pendingRegistrations.findByEmail(email)
                .filter(value -> !value.isExpired(Instant.now()))
                .ifPresent(value -> {
                    throw new ConflictException(
                            "Email verification is already pending. Request a new code instead.");
                });
        pendingRegistrations.deleteByEmail(email);
        PendingRegistration pending = new PendingRegistration(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.academicYear(),
                phone,
                request.gender(),
                Instant.now().plus(properties.getOtp().getTtl()));
        pendingRegistrations.save(pending);
        try {
            Instant otpExpiresAt = otpChallenges.issue(email, OtpPurpose.EMAIL_VERIFICATION);
            return new AuthResponses.RegistrationResponse(
                    email, otpExpiresAt, "Verification code sent to your email");
        } catch (RuntimeException exception) {
            pendingRegistrations.deleteByEmail(email);
            throw exception;
        }
    }

    public AuthResponses.TokenResponse verifyEmail(AuthRequests.VerifyEmailRequest request) {
        String email = normalizeEmail(request.email());
        PendingRegistration pending = pendingRegistrations.findByEmail(email)
                .filter(value -> !value.isExpired(Instant.now()))
                .orElseThrow(() -> new InvalidRequestException("Registration is missing or expired"));
        if (accounts.findByEmail(email).isPresent()) {
            throw new ConflictException("Account is already verified");
        }
        EmailOtpChallenge challenge = otpChallenges.validate(
                email, request.otp(), OtpPurpose.EMAIL_VERIFICATION);

        Student student = studentService.createAccountProfile(
                pending.getFullName(),
                email,
                pending.getAcademicYear(),
                pending.getPhoneNumber(),
                pending.getGender());
        AuthAccount account;
        try {
            account = accounts.save(AuthAccount.local(email, pending.getPasswordHash(), student.getId()));
        } catch (RuntimeException exception) {
            studentService.rollbackFailedAccountProfile(student.getId());
            if (exception instanceof DuplicateKeyException) {
                throw new ConflictException("Account is already verified", exception);
            }
            throw exception;
        }
        otpChallenges.consume(challenge);
        pendingRegistrations.deleteByEmail(email);
        return sessions.issue(account);
    }

    public AuthResponses.RegistrationResponse resendVerification(AuthRequests.EmailRequest request) {
        String email = normalizeEmail(request.email());
        PendingRegistration pending = pendingRegistrations.findByEmail(email)
                .filter(value -> !value.isExpired(Instant.now()))
                .orElseThrow(() -> new InvalidRequestException("Registration is missing or expired"));
        Instant expiresAt = otpChallenges.issue(pending.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
        return new AuthResponses.RegistrationResponse(
                email, expiresAt, "A new verification code was sent to your email");
    }

    public AuthResponses.TokenResponse login(AuthRequests.LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        CampusUserPrincipal principal = (CampusUserPrincipal) authentication.getPrincipal();
        AuthAccount account = requireAccount(principal.accountId());
        account.recordLogin();
        account = accounts.save(account);
        return sessions.issue(account);
    }

    public AuthResponses.MessageResponse requestPasswordReset(AuthRequests.EmailRequest request) {
        String email = normalizeEmail(request.email());
        accounts.findByEmail(email).filter(AuthAccount::isActive)
                .ifPresent(account -> otpChallenges.issue(email, OtpPurpose.PASSWORD_RESET));
        return new AuthResponses.MessageResponse(
                "If an active account exists, a password reset code has been sent");
    }

    public AuthResponses.MessageResponse resetPassword(AuthRequests.ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        AuthAccount account = accounts.findByEmail(email)
                .filter(AuthAccount::isActive)
                .orElseThrow(() -> new InvalidRequestException("The password reset code is invalid or expired"));
        EmailOtpChallenge challenge = otpChallenges.validate(
                email, request.otp(), OtpPurpose.PASSWORD_RESET);
        account.changePassword(passwordEncoder.encode(request.newPassword()));
        accounts.save(account);
        sessions.revokeAll(account.getId());
        otpChallenges.consume(challenge);
        return new AuthResponses.MessageResponse("Password reset successfully");
    }

    public AuthAccount loginWithGoogle(String rawEmail, String googleSubject) {
        String email = normalizeEmail(rawEmail);
        AuthAccount account = accounts.findByEmail(email).orElseGet(() -> {
            AuthAccount created = AuthAccount.google(email, googleSubject);
            studentService.findByEmail(email).ifPresent(student -> created.linkStudent(student.getId()));
            return created;
        });
        if (account.getGoogleSubject() != null && !account.getGoogleSubject().equals(googleSubject)) {
            throw new ConflictException("This email is already linked to another Google account");
        }
        account.linkGoogle(googleSubject);
        account.recordLogin();
        return accounts.save(account);
    }

    public String createOAuthExchangeCode(AuthAccount account) {
        return oauthTickets.create(account);
    }

    public AuthResponses.TokenResponse exchangeOAuthCode(String code) {
        return oauthTickets.exchange(code);
    }

    public AuthResponses.TokenResponse completeProfile(
            CampusUserPrincipal principal, AuthRequests.CompleteProfileRequest request) {
        requirePrincipal(principal);
        AuthAccount account = requireAccount(principal.accountId());
        if (account.isProfileComplete()) {
            throw new ConflictException("Student profile is already complete");
        }
        String phone = normalizePhone(request.phoneNumber());
        if (studentService.existsByPhoneNumber(phone)) {
            throw new ConflictException("A student profile already exists for this phone number");
        }
        Student student = studentService.createAccountProfile(
                request.fullName(), account.getEmail(),
                request.academicYear(), phone, request.gender());
        account.linkStudent(student.getId());
        account = accounts.save(account);
        sessions.revokeAll(account.getId());
        return sessions.issue(account);
    }

    public AuthResponses.AccountResponse currentAccount(CampusUserPrincipal principal) {
        requirePrincipal(principal);
        return accountResponse(requireAccount(principal.accountId()));
    }

    private void requirePrincipal(CampusUserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
    }

    private AuthAccount requireAccount(String accountId) {
        return accounts.findById(accountId)
                .filter(AuthAccount::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private AuthResponses.AccountResponse accountResponse(AuthAccount account) {
        return new AuthResponses.AccountResponse(
                account.getId(), account.getEmail(), account.getStudentId(),
                account.isProfileComplete(), Set.copyOf(account.getProviders()),
                Set.copyOf(account.getRoles()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber.replace(" ", "").replace("-", "");
    }
}
