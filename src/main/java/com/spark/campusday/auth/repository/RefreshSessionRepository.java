package com.spark.campusday.auth.repository;

import com.spark.campusday.auth.model.RefreshSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshSessionRepository extends MongoRepository<RefreshSession, String> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    List<RefreshSession> findByAccountIdAndRevokedAtIsNull(String accountId);
}
