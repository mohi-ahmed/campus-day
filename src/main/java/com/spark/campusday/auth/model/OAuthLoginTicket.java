package com.spark.campusday.auth.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "auth_oauth_login_tickets")
public class OAuthLoginTicket {
    @Id
    private String id;

    @Indexed(unique = true)
    private String codeHash;

    private String accountId;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private Instant consumedAt;

    @Version
    private Long version;

    protected OAuthLoginTicket() {
        // Spring Data MongoDB
    }

    public OAuthLoginTicket(String codeHash, String accountId, Instant expiresAt) {
        this.codeHash = codeHash;
        this.accountId = accountId;
        this.expiresAt = expiresAt;
    }

    public boolean canBeUsed(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }
}
