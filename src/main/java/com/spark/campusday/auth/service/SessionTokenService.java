package com.spark.campusday.auth.service;

import com.spark.campusday.auth.config.AuthProperties;
import com.spark.campusday.auth.dto.AuthResponses;
import com.spark.campusday.auth.model.AuthAccount;
import com.spark.campusday.auth.model.RefreshSession;
import com.spark.campusday.auth.repository.AuthAccountRepository;
import com.spark.campusday.auth.repository.RefreshSessionRepository;
import com.spark.campusday.auth.security.JwtTokenService;
import com.spark.campusday.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessionTokenService {
    private final JwtTokenService jwtTokens;
    private final SecretTokenService secrets;
    private final RefreshSessionRepository refreshSessions;
    private final AuthAccountRepository accounts;
    private final AuthProperties properties;

    public AuthResponses.TokenResponse issue(AuthAccount account) {
        JwtTokenService.IssuedJwt accessToken = jwtTokens.issue(account);
        String rawRefreshToken = secrets.newOpaqueToken();
        Instant refreshExpiresAt = Instant.now().plus(properties.getJwt().getRefreshTokenTtl());
        refreshSessions.save(new RefreshSession(
                secrets.hashToken(rawRefreshToken), account.getId(), refreshExpiresAt));
        return response(account, accessToken, rawRefreshToken, refreshExpiresAt);
    }

    public AuthResponses.TokenResponse rotate(String rawRefreshToken) {
        RefreshSession current = requireUsable(rawRefreshToken);
        current.revoke(Instant.now());
        try {
            refreshSessions.save(current);
        } catch (OptimisticLockingFailureException exception) {
            throw new InvalidRequestException("Refresh token is invalid or expired");
        }
        AuthAccount account = accounts.findById(current.getAccountId())
                .filter(AuthAccount::isActive)
                .orElseThrow(() -> new InvalidRequestException("Refresh token is invalid or expired"));
        return issue(account);
    }

    public void revoke(String rawRefreshToken) {
        refreshSessions.findByTokenHash(secrets.hashToken(rawRefreshToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.revoke(Instant.now());
                refreshSessions.save(session);
            }
        });
    }

    public void revokeAll(String accountId) {
        Instant now = Instant.now();
        var activeSessions = refreshSessions.findByAccountIdAndRevokedAtIsNull(accountId);
        activeSessions.forEach(session -> session.revoke(now));
        refreshSessions.saveAll(activeSessions);
    }

    private RefreshSession requireUsable(String rawRefreshToken) {
        return refreshSessions.findByTokenHash(secrets.hashToken(rawRefreshToken))
                .filter(session -> session.canBeUsed(Instant.now()))
                .orElseThrow(() -> new InvalidRequestException("Refresh token is invalid or expired"));
    }

    private AuthResponses.TokenResponse response(AuthAccount account,
                                                  JwtTokenService.IssuedJwt accessToken,
                                                  String refreshToken,
                                                  Instant refreshExpiresAt) {
        return new AuthResponses.TokenResponse(
                "Bearer",
                accessToken.value(),
                accessToken.expiresAt(),
                refreshToken,
                refreshExpiresAt,
                new AuthResponses.AccountResponse(
                        account.getId(), account.getEmail(), account.getStudentId(),
                        account.isProfileComplete(), Set.copyOf(account.getProviders()),
                        Set.copyOf(account.getRoles())));
    }
}
