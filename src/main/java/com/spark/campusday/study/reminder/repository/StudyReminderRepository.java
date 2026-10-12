package com.spark.campusday.study.reminder.repository;

import com.spark.campusday.study.reminder.model.StudyReminder;
import com.spark.campusday.study.reminder.model.StudyReminderStatus;
import com.spark.campusday.study.reminder.model.StudyReminderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudyReminderRepository extends MongoRepository<StudyReminder, String> {
    Optional<StudyReminder> findByIdAndStudentId(String id, String studentId);

    Optional<StudyReminder> findByStudentIdAndTypeAndSourceIdAndScheduledAt(
            String studentId,
            StudyReminderType type,
            String sourceId,
            Instant scheduledAt);

    List<StudyReminder> findByStudentIdAndStatusInOrderByScheduledAtDesc(
            String studentId, Collection<StudyReminderStatus> statuses);

    long countByStudentIdAndStatus(String studentId, StudyReminderStatus status);

    @Query(
            value = "{'studentId': ?0, 'deliveredAt': {'$gte': ?1, '$lt': ?2}}",
            count = true)
    long countDeliveredWithin(
            String studentId, Instant fromInclusive, Instant toExclusive);

    List<StudyReminder> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            StudyReminderStatus status, Instant scheduledAt, Pageable pageable);

    List<StudyReminder> findByStudentIdAndTypeAndSourceIdAndStatusOrderByScheduledAtAsc(
            String studentId,
            StudyReminderType type,
            String sourceId,
            StudyReminderStatus status);

    boolean existsByStudentIdAndTypeAndSourceIdAndScheduledAt(
            String studentId,
            StudyReminderType type,
            String sourceId,
            Instant scheduledAt);
}
