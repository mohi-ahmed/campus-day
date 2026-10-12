package com.spark.campusday.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private final Jwt jwt = new Jwt();
    private final Otp otp = new Otp();
    private final Brevo brevo = new Brevo();
    private URI frontendOauthSuccessUri;
    private URI frontendOauthFailureUri;

    @Getter
    @Setter
    public static class Jwt {
        private String issuer;
        private String secret;
        private Duration accessTokenTtl;
        private Duration refreshTokenTtl;
    }

    @Getter
    @Setter
    public static class Otp {
        private Duration ttl;
        private Duration resendCooldown;
        private int maxAttempts;
        private String pepper;
    }

    @Getter
    @Setter
    public static class Brevo {
        private String apiKey;
        private String baseUrl;
        private String senderEmail;
        private String senderName;
    }
}
