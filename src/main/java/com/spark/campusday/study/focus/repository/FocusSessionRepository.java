package com.spark.campusday.study.focus.repository;

import com.spark.campusday.study.focus.model.FocusSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends MongoRepository<FocusSession, String> {
    Optional<FocusSession> findByIdAndStudentId(String id, String studentId);

    List<FocusSession> findByStudentIdAndFocusDateOrderByStartedAtDesc(
            String studentId, LocalDate focusDate);

    @Query(
            value = "{ 'studentId': ?0, 'focusDate': { $gte: ?1, $lte: ?2 } }",
            sort = "{ 'startedAt': -1 }")
    List<FocusSession> findByStudentIdAndFocusDateGreaterThanEqualAndFocusDateLessThanEqualOrderByStartedAtDesc(
            String studentId, LocalDate from, LocalDate to);

    List<FocusSession> findByStudentIdAndTaskIdOrderByStartedAtDesc(
            String studentId, String taskId);
}
