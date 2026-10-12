package com.spark.campusday.auth.repository;

import com.spark.campusday.auth.model.EmailOtpChallenge;
import com.spark.campusday.auth.model.OtpPurpose;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmailOtpChallengeRepository extends MongoRepository<EmailOtpChallenge, String> {
    Optional<EmailOtpChallenge> findByEmailAndPurpose(String email, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}
