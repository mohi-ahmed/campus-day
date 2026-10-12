package com.spark.campusday.auth.repository;

import com.spark.campusday.auth.model.AuthAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AuthAccountRepository extends MongoRepository<AuthAccount, String> {
    Optional<AuthAccount> findByEmail(String email);
}
