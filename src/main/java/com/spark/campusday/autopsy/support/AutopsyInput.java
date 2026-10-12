package com.spark.campusday.autopsy.support;

import com.spark.campusday.common.exception.InvalidRequestException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AutopsyInput {
    private AutopsyInput() {
    }

    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new InvalidRequestException("Page must be zero or greater and size must be between 1 and 50");
        }
    }

    public static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String nickname(String value) {
        String normalized = optional(value);
        return normalized == null ? "Anonymous" : normalized;
    }

    public static String tag(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            return null;
        }
        String withoutHash = normalized.startsWith("#") ? normalized.substring(1).trim() : normalized;
        return withoutHash.isEmpty() ? null : "#" + withoutHash;
    }

    public static String actorHash(String sessionId) {
        String normalized = optional(sessionId);
        if (normalized == null) {
            throw new InvalidRequestException("Anonymous session ID is required");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
