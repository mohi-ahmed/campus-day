package com.spark.campusday.meal.absence.repository;

import com.spark.campusday.meal.absence.model.MealAbsenceNotice;
import com.spark.campusday.meal.model.MealType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MealAbsenceNoticeRepository extends MongoRepository<MealAbsenceNotice, String> {
    @Query(value = "{'studentId': ?0, 'startDate': {'$lte': ?2}, 'endDate': {'$gte': ?1}, 'meals': {'$in': ?3}}",
            exists = true)
    boolean existsOverlapping(String studentId, LocalDate startDate, LocalDate endDate, List<MealType> meals);

    @Query(value = "{'studentId': ?0, 'startDate': {'$lte': ?2}, 'endDate': {'$gte': ?1}}",
            sort = "{'startDate': -1, 'createdAt': -1}")
    List<MealAbsenceNotice> findInRange(String studentId, LocalDate from, LocalDate to);

    @Query("{'studentId': {'$in': ?0}, 'startDate': {'$lte': ?1}, 'endDate': {'$gte': ?1}}")
    List<MealAbsenceNotice> findActiveForStudents(Collection<String> studentIds, LocalDate date);
}
