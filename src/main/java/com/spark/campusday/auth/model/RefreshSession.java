package com.spark.campusday.auth.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "auth_refresh_sessions")
public class RefreshSession {
    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String accountId;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private Instant revokedAt;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    protected RefreshSession() {
        // Spring Data MongoDB
    }

    public RefreshSession(String tokenHash, String accountId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.accountId = accountId;
        this.expiresAt = expiresAt;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public boolean canBeUsed(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
