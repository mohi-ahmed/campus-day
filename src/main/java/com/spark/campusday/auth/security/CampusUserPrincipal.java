package com.spark.campusday.auth.security;

import com.spark.campusday.auth.model.AuthAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public record CampusUserPrincipal(
        String accountId,
        String email,
        String passwordHash,
        String studentId,
        boolean profileComplete,
        Set<String> roles,
        boolean enabled
) implements UserDetails {
    public static CampusUserPrincipal from(AuthAccount account) {
        return new CampusUserPrincipal(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getStudentId(),
                account.isProfileComplete(),
                Set.copyOf(account.getRoles()),
                account.isActive());
    }

    public static CampusUserPrincipal fromJwt(JwtTokenService.AccessTokenClaims claims) {
        return new CampusUserPrincipal(
                claims.accountId(), claims.email(), "", claims.studentId(),
                claims.profileComplete(), Set.copyOf(claims.roles()), true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
