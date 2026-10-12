package com.spark.campusday.study.reminder.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.preferences.service.StudyPreferencesService;
import com.spark.campusday.study.reminder.dto.StudyReminderInboxResponse;
import com.spark.campusday.study.reminder.dto.StudyReminderResponse;
import com.spark.campusday.study.reminder.dto.StudyReminderScheduleRequest;
import com.spark.campusday.study.reminder.exception.DuplicateStudyReminderException;
import com.spark.campusday.study.reminder.exception.InvalidStudyReminderStateException;
import com.spark.campusday.study.reminder.exception.StudyReminderNotFoundException;
import com.spark.campusday.study.reminder.model.StudyReminder;
import com.spark.campusday.study.reminder.model.StudyReminderStatus;
import com.spark.campusday.study.reminder.model.StudyReminderType;
import com.spark.campusday.study.reminder.repository.StudyReminderRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyReminderService {
    private static final int MAX_DELIVERY_BATCH_SIZE = 500;
    private static final List<StudyReminderStatus> INBOX_STATUSES = List.of(
            StudyReminderStatus.SCHEDULED,
            StudyReminderStatus.DELIVERED,
            StudyReminderStatus.READ);

    private final StudyReminderRepository reminders;
    private final StudentService students;
    private final StudyPreferencesService preferences;
    private final Validator validator;
    @Qualifier("studyClock")
    private final Clock clock;

    public StudyReminderResponse schedule(String studentId,
                                          StudyReminderScheduleRequest request) {
        validateRequest(request);
        return upsertForOwner(ownerId(studentId), request);
    }

    /** Reconciles the still-scheduled reminders for one domain source. */
    public List<StudyReminderResponse> replaceScheduledForOwner(
            String ownerId,
            StudyReminderType type,
            String sourceId,
            List<StudyReminderScheduleRequest> desiredRequests) {
        if (ownerId == null || ownerId.isBlank() || type == null
                || sourceId == null || sourceId.isBlank() || desiredRequests == null) {
            throw new InvalidRequestException(
                    "Reminder owner, type, source and desired schedule are required");
        }

        String normalizedSourceId = sourceId.trim();
        desiredRequests.forEach(request -> {
            validateRequest(request);
            if (request.type() != type
                    || !request.sourceId().trim().equals(normalizedSourceId)) {
                throw new InvalidRequestException(
                        "Every desired reminder must match its source and type");
            }
        });

        Set<Instant> desiredTimes = desiredRequests.stream()
                .map(StudyReminderScheduleRequest::scheduledAt)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<StudyReminder> obsolete = reminders
                .findByStudentIdAndTypeAndSourceIdAndStatusOrderByScheduledAtAsc(
                        ownerId, type, normalizedSourceId, StudyReminderStatus.SCHEDULED)
                .stream()
                .filter(reminder -> !desiredTimes.contains(reminder.getScheduledAt()))
                .toList();
        Instant cancelledAt = now();
        obsolete.forEach(reminder -> cancel(reminder, cancelledAt));
        if (!obsolete.isEmpty()) {
            reminders.saveAll(obsolete);
        }

        return desiredRequests.stream()
                .map(request -> upsertForOwner(ownerId, request))
                .toList();
    }

    public StudyReminderResponse get(String studentId, String reminderId) {
        return response(requireReminder(studentId, reminderId));
    }

    public StudyReminderInboxResponse inbox(String studentId) {
        String ownerId = ownerId(studentId);
        List<StudyReminderResponse> result = reminders
                .findByStudentIdAndStatusInOrderByScheduledAtDesc(ownerId, INBOX_STATUSES)
                .stream()
                .map(this::response)
                .toList();

        long unreadCount = reminders.countByStudentIdAndStatus(
                ownerId, StudyReminderStatus.DELIVERED);
        long scheduledCount = reminders.countByStudentIdAndStatus(
                ownerId, StudyReminderStatus.SCHEDULED);
        return new StudyReminderInboxResponse(unreadCount, scheduledCount, result);
    }

    public StudyReminderResponse markRead(String studentId, String reminderId) {
        StudyReminder reminder = requireReminder(studentId, reminderId);
        if (reminder.getStatus() == StudyReminderStatus.READ) {
            return response(reminder);
        }
        requireStatus(reminder, StudyReminderStatus.DELIVERED,
                "Only a delivered reminder can be marked as read");

        reminder.setStatus(StudyReminderStatus.READ);
        reminder.setReadAt(now());
        return response(reminders.save(reminder));
    }

    public StudyReminderResponse dismiss(String studentId, String reminderId) {
        StudyReminder reminder = requireReminder(studentId, reminderId);
        if (reminder.getStatus() == StudyReminderStatus.DISMISSED) {
            return response(reminder);
        }
        if (reminder.getStatus() != StudyReminderStatus.DELIVERED
                && reminder.getStatus() != StudyReminderStatus.READ) {
            throw new InvalidStudyReminderStateException(
                    "Only a delivered or read reminder can be dismissed");
        }

        reminder.setStatus(StudyReminderStatus.DISMISSED);
        reminder.setDismissedAt(now());
        return response(reminders.save(reminder));
    }

    public StudyReminderResponse cancel(String studentId, String reminderId) {
        StudyReminder reminder = requireReminder(studentId, reminderId);
        if (reminder.getStatus() == StudyReminderStatus.CANCELLED) {
            return response(reminder);
        }
        reminder.setUserCancelled(true);
        cancel(reminder, now());
        return response(reminders.save(reminder));
    }

    public int cancelScheduledForSource(String studentId, StudyReminderType type,
                                        String sourceId) {
        return cancelScheduledForSourceByOwner(ownerId(studentId), type, sourceId);
    }

    public int cancelScheduledForSourceByOwner(String ownerId, StudyReminderType type,
                                               String sourceId) {
        if (ownerId == null || ownerId.isBlank() || type == null
                || sourceId == null || sourceId.isBlank()) {
            throw new InvalidRequestException("Reminder owner, type and source are required");
        }

        List<StudyReminder> scheduled = reminders
                .findByStudentIdAndTypeAndSourceIdAndStatusOrderByScheduledAtAsc(
                        ownerId, type, sourceId.trim(),
                        StudyReminderStatus.SCHEDULED);
        Instant cancelledAt = now();
        scheduled.forEach(reminder -> cancel(reminder, cancelledAt));
        if (!scheduled.isEmpty()) {
            reminders.saveAll(scheduled);
        }
        return scheduled.size();
    }

    /** Called by reminder-delivery infrastructure in bounded batches. */
    public List<StudyReminderResponse> deliverDue(int batchSize) {
        if (batchSize < 1 || batchSize > MAX_DELIVERY_BATCH_SIZE) {
            throw new InvalidRequestException(
                    "Reminder delivery batch size must be between 1 and "
                            + MAX_DELIVERY_BATCH_SIZE);
        }

        Instant deliveredAt = now();
        LocalDate deliveryDate = LocalDate.now(clock);
        Instant dayStart = deliveryDate.atStartOfDay(clock.getZone()).toInstant();
        Instant nextDayStart = deliveryDate.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        List<StudyReminder> due = reminders
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        StudyReminderStatus.SCHEDULED, deliveredAt,
                        PageRequest.of(0, batchSize));
        Map<String, Long> deliveredCounts = new HashMap<>();
        due.forEach(reminder -> {
            if (reminder.getType() == StudyReminderType.MORNING_DIGEST) {
                deliver(reminder, deliveredAt);
                return;
            }
            long deliveredCount = deliveredCounts.computeIfAbsent(
                    reminder.getStudentId(), ownerId -> reminders
                            .countDeliveredWithin(
                                    ownerId, dayStart, nextDayStart));
            int dailyLimit = preferences.getForOwner(reminder.getStudentId())
                    .dailyReminderLimit();
            if (deliveredCount >= dailyLimit) {
                reminder.setStatus(StudyReminderStatus.DISMISSED);
                reminder.setDismissedAt(deliveredAt);
                return;
            }
            deliver(reminder, deliveredAt);
            deliveredCounts.put(reminder.getStudentId(), deliveredCount + 1);
        });
        if (due.isEmpty()) {
            return List.of();
        }
        return reminders.saveAll(due).stream().map(this::response).toList();
    }

    private StudyReminder requireReminder(String studentId, String reminderId) {
        if (reminderId == null || reminderId.isBlank()) {
            throw new InvalidRequestException("Reminder id is required");
        }
        return reminders.findByIdAndStudentId(reminderId, ownerId(studentId))
                .orElseThrow(() -> new StudyReminderNotFoundException(reminderId));
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(StudyReminderScheduleRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Study reminder request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(StudyReminder reminder) {
        var violations = validator.validate(reminder);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private StudyReminder save(StudyReminder reminder) {
        try {
            return reminders.save(reminder);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateStudyReminderException(reminder.getSourceId(), exception);
        }
    }

    private StudyReminderResponse upsertForOwner(
            String ownerId, StudyReminderScheduleRequest request) {
        String sourceId = request.sourceId().trim();
        StudyReminder reminder = reminders
                .findByStudentIdAndTypeAndSourceIdAndScheduledAt(
                        ownerId, request.type(), sourceId, request.scheduledAt())
                .orElseGet(() -> StudyReminder.builder()
                        .studentId(ownerId)
                        .type(request.type())
                        .sourceId(sourceId)
                        .scheduledAt(request.scheduledAt())
                        .status(StudyReminderStatus.SCHEDULED)
                        .build());

        if (reminder.getStatus() == StudyReminderStatus.CANCELLED
                && !reminder.isUserCancelled()
                && request.scheduledAt().isAfter(now())) {
            reminder.setStatus(StudyReminderStatus.SCHEDULED);
            reminder.setCancelledAt(null);
            reminder.setUserCancelled(false);
        } else if (reminder.getStatus() != StudyReminderStatus.SCHEDULED) {
            return response(reminder);
        }

        reminder.setTitle(request.title().trim());
        reminder.setMessage(request.message().trim());
        reminder.setActionPath(normalize(request.actionPath()));
        validate(reminder);
        return response(save(reminder));
    }

    private void requireStatus(StudyReminder reminder, StudyReminderStatus required,
                               String message) {
        if (reminder.getStatus() != required) {
            throw new InvalidStudyReminderStateException(message);
        }
    }

    private void deliver(StudyReminder reminder, Instant deliveredAt) {
        requireStatus(reminder, StudyReminderStatus.SCHEDULED,
                "Only a scheduled reminder can be delivered");
        reminder.setStatus(StudyReminderStatus.DELIVERED);
        reminder.setDeliveredAt(deliveredAt);
    }

    private void cancel(StudyReminder reminder, Instant cancelledAt) {
        requireStatus(reminder, StudyReminderStatus.SCHEDULED,
                "Only a scheduled reminder can be cancelled");
        reminder.setStatus(StudyReminderStatus.CANCELLED);
        reminder.setCancelledAt(cancelledAt);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private StudyReminderResponse response(StudyReminder reminder) {
        return new StudyReminderResponse(reminder.getId(), reminder.getType(),
                reminder.getSourceId(), reminder.getTitle(), reminder.getMessage(),
                reminder.getActionPath(), reminder.getScheduledAt(), reminder.getStatus(),
                reminder.getDeliveredAt(), reminder.getReadAt(), reminder.getDismissedAt(),
                reminder.getCancelledAt(), reminder.getCreatedAt(), reminder.getUpdatedAt());
    }
}
