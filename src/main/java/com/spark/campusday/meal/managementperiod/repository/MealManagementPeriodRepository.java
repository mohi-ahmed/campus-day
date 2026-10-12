package com.spark.campusday.meal.managementperiod.repository;

import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MealManagementPeriodRepository extends MongoRepository<MealManagementPeriod, String> {
    Optional<MealManagementPeriod> findByYearAndMonth(int year, int month);
    Optional<MealManagementPeriod> findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            ManagementPeriodStatus status, LocalDate dateAtOrAfterStart, LocalDate dateAtOrBeforeEnd);

    boolean existsByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            ManagementPeriodStatus status, LocalDate proposedEnd, LocalDate proposedStart, String excludedId);

    Optional<MealManagementPeriod> findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate startDate);

    Optional<MealManagementPeriod> findFirstByStartDateGreaterThanOrderByStartDateAsc(LocalDate startDate);
}
