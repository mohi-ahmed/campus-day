package com.spark.campusday.auth.security;

import com.spark.campusday.auth.config.AuthProperties;
import com.spark.campusday.auth.model.AuthAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {
    private static final int HS512_MINIMUM_KEY_BYTES = 64;

    private final AuthProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(AuthProperties properties) {
        this.properties = properties;
        byte[] secretBytes = Decoders.BASE64.decode(properties.getJwt().getSecret());
        if (secretBytes.length < HS512_MINIMUM_KEY_BYTES) {
            throw new IllegalStateException(
                    "AUTH_JWT_SECRET must be a Base64-encoded value containing at least 64 bytes for HS512");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public IssuedJwt issue(AuthAccount account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());
        String token = Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(account.getId())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("email", account.getEmail())
                .claim("studentId", account.getStudentId())
                .claim("profileComplete", account.isProfileComplete())
                .claim("roles", account.getRoles())
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
        return new IssuedJwt(token, expiresAt);
    }

    public AccessTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object roleClaim = claims.get("roles");
        List<String> roles = roleClaim instanceof Collection<?> values
                ? values.stream().map(String::valueOf).toList()
                : List.of();
        return new AccessTokenClaims(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("studentId", String.class),
                Boolean.TRUE.equals(claims.get("profileComplete", Boolean.class)),
                roles);
    }

    public record IssuedJwt(String value, Instant expiresAt) {
    }

    public record AccessTokenClaims(
            String accountId,
            String email,
            String studentId,
            boolean profileComplete,
            List<String> roles
    ) {
    }
}
