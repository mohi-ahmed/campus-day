package com.spark.campusday.autopsy.support;

import com.spark.campusday.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class AnonymousActorFingerprint {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec fingerprintKey;

    public AnonymousActorFingerprint() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        fingerprintKey = new SecretKeySpec(secret, HMAC_ALGORITHM);
    }

    public String create(String sessionId, String clientIp, String userAgent) {
        String normalizedSession = requireSessionId(sessionId);
        String normalizedIp = normalize(clientIp);
        String normalizedUserAgent = normalize(userAgent);
        String source = part(normalizedSession) + part(normalizedIp) + part(normalizedUserAgent);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(fingerprintKey);
            return HexFormat.of().formatHex(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create anonymous rate-limit key", exception);
        }
    }

    private String requireSessionId(String sessionId) {
        String normalized = AutopsyInput.optional(sessionId);
        if (normalized == null) {
            throw new InvalidRequestException("Anonymous session ID is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        String normalized = AutopsyInput.optional(value);
        return normalized == null ? "unknown" : normalized;
    }

    private String part(String value) {
        return value.length() + ":" + value;
    }
}
