package com.spark.campusday.study.habit.repository;

import com.spark.campusday.study.habit.model.StudyHabitCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyHabitCompletionRepository
        extends MongoRepository<StudyHabitCompletion, String> {
    Optional<StudyHabitCompletion> findByHabitIdAndStudentIdAndCompletedOn(
            String habitId, String studentId, LocalDate completedOn);

    @Query(
            value = "{ 'studentId': ?0, 'completedOn': { $gte: ?1, $lte: ?2 } }",
            sort = "{ 'completedOn': 1 }")
    List<StudyHabitCompletion> findByStudentIdAndCompletedOnGreaterThanEqualAndCompletedOnLessThanEqualOrderByCompletedOnAsc(
            String studentId, LocalDate from, LocalDate to);

    @Query(
            value = "{ 'habitId': ?0, 'studentId': ?1, 'completedOn': { $gte: ?2, $lte: ?3 } }",
            sort = "{ 'completedOn': 1 }")
    List<StudyHabitCompletion> findByHabitIdAndStudentIdAndCompletedOnGreaterThanEqualAndCompletedOnLessThanEqualOrderByCompletedOnAsc(
            String habitId, String studentId, LocalDate from, LocalDate to);

    boolean existsByHabitIdAndStudentIdAndCompletedOn(
            String habitId, String studentId, LocalDate completedOn);

    long deleteByHabitIdAndStudentIdAndCompletedOn(
            String habitId, String studentId, LocalDate completedOn);

    long deleteByHabitIdAndStudentId(String habitId, String studentId);
}
