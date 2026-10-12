package com.spark.campusday.auth.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

@Getter
@Document(collection = "auth_email_otp_challenges")
@CompoundIndex(name = "email_purpose_idx", def = "{'email': 1, 'purpose': 1}", unique = true)
public class EmailOtpChallenge {
    @Id
    private String id;

    private String email;
    private OtpPurpose purpose;
    private String otpHash;
    private int failedAttempts;
    private Instant consumedAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    @CreatedDate
    private Instant createdAt;

    protected EmailOtpChallenge() {
        // Spring Data MongoDB
    }

    public EmailOtpChallenge(String email, OtpPurpose purpose, String otpHash, Instant expiresAt) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.purpose = purpose;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public void recordFailure() {
        this.failedAttempts++;
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
