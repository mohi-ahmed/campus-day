package com.spark.campusday.auth.service;

import com.spark.campusday.auth.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class SecretTokenService {
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String newOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String newOtp() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    public String hashToken(String token) {
        return sha256(token);
    }

    public String hashOtp(String email, String otp) {
        return sha256(email.trim().toLowerCase(java.util.Locale.ROOT)
                + ":" + otp + ":" + properties.getOtp().getPepper());
    }

    public boolean hashesMatch(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
