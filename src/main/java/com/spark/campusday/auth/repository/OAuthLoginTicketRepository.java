package com.spark.campusday.auth.repository;

import com.spark.campusday.auth.model.OAuthLoginTicket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OAuthLoginTicketRepository extends MongoRepository<OAuthLoginTicket, String> {
    Optional<OAuthLoginTicket> findByCodeHash(String codeHash);
}
