package com.spark.campusday.auth.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Getter
@Document(collection = "auth_accounts")
public class AuthAccount {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    @Indexed(unique = true, sparse = true)
    private String googleSubject;
    private String studentId;
    private boolean profileComplete;
    private AccountStatus status;
    private Set<AuthProvider> providers;
    private Set<String> roles;
    private Instant lastLoginAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected AuthAccount() {
        // Spring Data MongoDB
    }

    private AuthAccount(String email) {
        this.email = normalizeEmail(email);
        this.status = AccountStatus.ACTIVE;
        this.providers = new HashSet<>();
        this.roles = new HashSet<>(Set.of("ROLE_STUDENT"));
    }

    public static AuthAccount local(String email, String passwordHash, String studentId) {
        AuthAccount account = new AuthAccount(email);
        account.passwordHash = passwordHash;
        account.studentId = studentId;
        account.profileComplete = true;
        account.providers.add(AuthProvider.LOCAL);
        return account;
    }

    public static AuthAccount google(String email, String googleSubject) {
        AuthAccount account = new AuthAccount(email);
        account.googleSubject = googleSubject;
        account.providers.add(AuthProvider.GOOGLE);
        return account;
    }

    public void linkGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
        this.providers.add(AuthProvider.GOOGLE);
    }

    public void linkStudent(String studentId) {
        this.studentId = studentId;
        this.profileComplete = true;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.providers.add(AuthProvider.LOCAL);
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
