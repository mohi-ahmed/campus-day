package com.spark.campusday.auth.service;

import com.spark.campusday.auth.config.AuthProperties;
import com.spark.campusday.auth.mail.OtpEmailSender;
import com.spark.campusday.auth.model.EmailOtpChallenge;
import com.spark.campusday.auth.model.OtpPurpose;
import com.spark.campusday.auth.repository.EmailOtpChallengeRepository;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OtpChallengeService {
    private final EmailOtpChallengeRepository challenges;
    private final SecretTokenService secrets;
    private final OtpEmailSender emailSender;
    private final AuthProperties properties;

    public Instant issue(String rawEmail, OtpPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        Instant now = Instant.now();
        challenges.findByEmailAndPurpose(email, purpose).ifPresent(existing -> {
            if (existing.getCreatedAt() != null
                    && existing.getCreatedAt().plus(properties.getOtp().getResendCooldown()).isAfter(now)) {
                throw new RateLimitExceededException("Please wait before requesting another code");
            }
        });
        String otp = secrets.newOtp();
        Instant expiresAt = now.plus(properties.getOtp().getTtl());
        challenges.deleteByEmailAndPurpose(email, purpose);
        EmailOtpChallenge challenge = challenges.save(new EmailOtpChallenge(
                email, purpose, secrets.hashOtp(email, otp), expiresAt));
        try {
            emailSender.send(email, otp, purpose);
            return expiresAt;
        } catch (RuntimeException exception) {
            challenges.deleteById(challenge.getId());
            throw exception;
        }
    }

    public EmailOtpChallenge validate(String rawEmail, String otp, OtpPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        EmailOtpChallenge challenge = challenges.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> invalidOtp(purpose));
        Instant now = Instant.now();
        if (challenge.getConsumedAt() != null || challenge.isExpired(now)
                || challenge.getFailedAttempts() >= properties.getOtp().getMaxAttempts()) {
            throw invalidOtp(purpose);
        }
        String suppliedHash = secrets.hashOtp(email, otp);
        if (!secrets.hashesMatch(challenge.getOtpHash(), suppliedHash)) {
            challenge.recordFailure();
            challenges.save(challenge);
            throw invalidOtp(purpose);
        }
        return challenge;
    }

    public void consume(EmailOtpChallenge challenge) {
        challenge.consume(Instant.now());
        challenges.save(challenge);
    }

    private InvalidRequestException invalidOtp(OtpPurpose purpose) {
        String label = purpose == OtpPurpose.EMAIL_VERIFICATION ? "verification" : "password reset";
        return new InvalidRequestException("The " + label + " code is invalid or expired");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
