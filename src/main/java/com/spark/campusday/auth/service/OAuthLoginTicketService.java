package com.spark.campusday.auth.service;

import com.spark.campusday.auth.dto.AuthResponses;
import com.spark.campusday.auth.model.AuthAccount;
import com.spark.campusday.auth.model.OAuthLoginTicket;
import com.spark.campusday.auth.repository.AuthAccountRepository;
import com.spark.campusday.auth.repository.OAuthLoginTicketRepository;
import com.spark.campusday.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OAuthLoginTicketService {
    private static final Duration TICKET_TTL = Duration.ofMinutes(2);

    private final OAuthLoginTicketRepository tickets;
    private final AuthAccountRepository accounts;
    private final SecretTokenService secrets;
    private final SessionTokenService sessions;

    public String create(AuthAccount account) {
        String rawCode = secrets.newOpaqueToken();
        tickets.save(new OAuthLoginTicket(
                secrets.hashToken(rawCode), account.getId(), Instant.now().plus(TICKET_TTL)));
        return rawCode;
    }

    public AuthResponses.TokenResponse exchange(String rawCode) {
        OAuthLoginTicket ticket = tickets.findByCodeHash(secrets.hashToken(rawCode))
                .filter(value -> value.canBeUsed(Instant.now()))
                .orElseThrow(() -> new InvalidRequestException("OAuth exchange code is invalid or expired"));
        ticket.consume(Instant.now());
        try {
            tickets.save(ticket);
        } catch (OptimisticLockingFailureException exception) {
            throw new InvalidRequestException("OAuth exchange code is invalid or expired");
        }
        AuthAccount account = accounts.findById(ticket.getAccountId())
                .filter(AuthAccount::isActive)
                .orElseThrow(() -> new InvalidRequestException("OAuth account is unavailable"));
        return sessions.issue(account);
    }
}
