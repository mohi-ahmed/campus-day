package com.spark.campusday.auth.security;

import com.spark.campusday.auth.repository.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CampusUserDetailsService implements UserDetailsService {
    private final AuthAccountRepository accounts;

    @Override
    public UserDetails loadUserByUsername(String rawEmail) throws UsernameNotFoundException {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        return accounts.findByEmail(email)
                .filter(account -> account.isActive() && account.getPasswordHash() != null)
                .map(CampusUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
