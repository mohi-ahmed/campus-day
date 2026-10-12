package com.spark.campusday.meal.managerapplication.repository;

import com.spark.campusday.meal.managerapplication.model.MealManagerApplication;
import com.spark.campusday.meal.managerapplication.model.MealManagerApplicationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MealManagerApplicationRepository extends MongoRepository<MealManagerApplication, String> {
    Optional<MealManagerApplication> findByTokenHash(String tokenHash);

    Optional<MealManagerApplication> findByStudentDocumentIdAndManagementPeriodId(
            String studentDocumentId, String managementPeriodId);

    boolean existsByStudentDocumentIdAndManagementPeriodIdAndStatus(
            String studentDocumentId,
            String managementPeriodId,
            MealManagerApplicationStatus status);

    List<MealManagerApplication> findAllByStatusAndExpiresAtLessThanEqual(
            MealManagerApplicationStatus status, Instant expiresAt);
}
