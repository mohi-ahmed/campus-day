package com.spark.campusday.auth.repository;

import com.spark.campusday.auth.model.PendingRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PendingRegistrationRepository extends MongoRepository<PendingRegistration, String> {
    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}
