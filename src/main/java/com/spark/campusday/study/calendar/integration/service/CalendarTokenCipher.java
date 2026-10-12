package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.study.calendar.integration.config.CalendarIntegrationProperties;
import com.spark.campusday.study.calendar.integration.exception.CalendarIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CalendarTokenCipher {
    private static final int IV_BYTES = 12;
    private static final int AUTH_TAG_BITS = 128;

    private final CalendarIntegrationProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new CalendarIntegrationException("Calendar credentials could not be secured");
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(value);
            if (payload.length <= IV_BYTES) {
                throw new GeneralSecurityException("Encrypted token is incomplete");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            return new String(cipher(Cipher.DECRYPT_MODE, iv).doFinal(encrypted),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new CalendarIntegrationException(
                    "Calendar credentials could not be read. Reconnect Google Calendar");
        }
    }

    private Cipher cipher(int mode, byte[] iv) throws GeneralSecurityException {
        String secret = properties.tokenEncryptionKey();
        if (secret == null || secret.isBlank()) {
            throw new CalendarIntegrationException(
                    "Calendar token encryption key is not configured");
        }
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(AUTH_TAG_BITS, iv));
        return cipher;
    }
}
