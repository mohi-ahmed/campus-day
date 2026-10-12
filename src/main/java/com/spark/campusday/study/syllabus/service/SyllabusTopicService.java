package com.spark.campusday.study.syllabus.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.integration.service.StudyCalendarAutoSyncService;
import com.spark.campusday.study.syllabus.dto.SyllabusTopicRequest;
import com.spark.campusday.study.syllabus.dto.SyllabusTopicResponse;
import com.spark.campusday.study.syllabus.dto.SyllabusTopicStatusRequest;
import com.spark.campusday.study.syllabus.exception.DuplicateSyllabusTopicException;
import com.spark.campusday.study.syllabus.exception.InvalidSyllabusStateException;
import com.spark.campusday.study.syllabus.exception.SyllabusSubjectNotFoundException;
import com.spark.campusday.study.syllabus.exception.SyllabusTopicNotFoundException;
import com.spark.campusday.study.syllabus.model.StudySyllabusSubject;
import com.spark.campusday.study.syllabus.model.StudySyllabusTopic;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.repository.StudySyllabusSubjectRepository;
import com.spark.campusday.study.syllabus.repository.StudySyllabusTopicRepository;
import com.spark.campusday.study.reminder.service.StudyReminderSchedulingService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SyllabusTopicService {
    private final StudySyllabusTopicRepository topics;
    private final StudySyllabusSubjectRepository subjects;
    private final StudentService students;
    private final Validator validator;
    private final StudyReminderSchedulingService reminderScheduling;
    private final StudyCalendarAutoSyncService calendarAutoSync;

    public SyllabusTopicResponse create(String studentId, String subjectId,
                                        SyllabusTopicRequest request) {
        validateRequest(request);
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        requireActive(subject);
        String titleKey = titleKey(request.title());
        if (topics.existsBySyllabusSubjectIdAndTitleKey(subject.getId(), titleKey)) {
            throw new DuplicateSyllabusTopicException(request.title().trim());
        }

        StudySyllabusTopic topic = StudySyllabusTopic.builder()
                .studentId(subject.getStudentId())
                .syllabusSubjectId(subject.getId())
                .title(request.title().trim())
                .titleKey(titleKey)
                .note(normalize(request.note()))
                .status(SyllabusTopicStatus.NOT_STARTED)
                .displayOrder(request.displayOrder())
                .targetDate(request.targetDate())
                .build();
        validate(topic);
        StudySyllabusTopic saved = save(topic);
        reminderScheduling.syncSyllabusTopicSafely(saved);
        calendarAutoSync.syncDatesSafely(studentId, saved.getTargetDate());
        return response(saved);
    }

    public SyllabusTopicResponse update(String studentId, String subjectId, String topicId,
                                        SyllabusTopicRequest request) {
        validateRequest(request);
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        requireActive(subject);
        StudySyllabusTopic topic = requireTopic(subject, topicId);
        LocalDate previousTargetDate = topic.getTargetDate();
        String titleKey = titleKey(request.title());
        if (topics.existsBySyllabusSubjectIdAndTitleKeyAndIdNot(
                subject.getId(), titleKey, topic.getId())) {
            throw new DuplicateSyllabusTopicException(request.title().trim());
        }

        topic.setTitle(request.title().trim());
        topic.setTitleKey(titleKey);
        topic.setNote(normalize(request.note()));
        topic.setTargetDate(request.targetDate());
        topic.setDisplayOrder(request.displayOrder());
        validate(topic);
        StudySyllabusTopic saved = save(topic);
        reminderScheduling.syncSyllabusTopicSafely(saved);
        calendarAutoSync.syncDatesSafely(
                studentId, previousTargetDate, saved.getTargetDate());
        return response(saved);
    }

    public SyllabusTopicResponse get(String studentId, String subjectId, String topicId) {
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        return response(requireTopic(subject, topicId));
    }

    public List<SyllabusTopicResponse> list(String studentId, String subjectId) {
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        return topics.findBySyllabusSubjectIdAndStudentIdOrderByDisplayOrderAscTitleAsc(
                        subject.getId(), subject.getStudentId())
                .stream().map(this::response).toList();
    }

    public List<SyllabusTopicResponse> listByStatus(String studentId,
                                                    SyllabusTopicStatus status) {
        if (status == null) {
            throw new InvalidRequestException("Syllabus topic status is required");
        }
        return topics.findByStudentIdAndStatusOrderByDisplayOrderAscTitleAsc(
                        ownerId(studentId), status)
                .stream().map(this::response).toList();
    }

    public List<SyllabusTopicResponse> listByTargetDate(String studentId, LocalDate from,
                                                        LocalDate to) {
        validateRange(from, to);
        return topics.findByStudentIdAndTargetDateGreaterThanEqualAndTargetDateLessThanEqualOrderByTargetDateAscDisplayOrderAsc(
                        ownerId(studentId), from, to)
                .stream().map(this::response).toList();
    }

    public SyllabusTopicResponse updateStatus(String studentId, String subjectId, String topicId,
                                              SyllabusTopicStatusRequest request) {
        validateStatusRequest(request);
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        requireActive(subject);
        StudySyllabusTopic topic = requireTopic(subject, topicId);
        if (topic.getStatus() == request.status()) {
            throw new InvalidSyllabusStateException(
                    "Syllabus topic already has status " + request.status());
        }

        topic.setStatus(request.status());
        topic.setCompletedAt(request.status() == SyllabusTopicStatus.COMPLETED
                ? Instant.now() : null);
        StudySyllabusTopic saved = topics.save(topic);
        reminderScheduling.syncSyllabusTopicSafely(saved);
        calendarAutoSync.syncDatesSafely(studentId, saved.getTargetDate());
        return response(saved);
    }

    public void delete(String studentId, String subjectId, String topicId) {
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        StudySyllabusTopic topic = requireTopic(subject, topicId);
        reminderScheduling.cancelSyllabusTopicSafely(topic);
        topics.delete(topic);
        calendarAutoSync.syncDatesSafely(studentId, topic.getTargetDate());
    }

    private StudySyllabusSubject requireSubject(String studentId, String subjectId) {
        return subjects.findByIdAndStudentId(subjectId, ownerId(studentId))
                .orElseThrow(() -> new SyllabusSubjectNotFoundException(subjectId));
    }

    private StudySyllabusTopic requireTopic(StudySyllabusSubject subject, String topicId) {
        return topics.findByIdAndSyllabusSubjectIdAndStudentId(
                        topicId, subject.getId(), subject.getStudentId())
                .orElseThrow(() -> new SyllabusTopicNotFoundException(topicId));
    }

    private void requireActive(StudySyllabusSubject subject) {
        if (!subject.isActive()) {
            throw new InvalidSyllabusStateException(
                    "Topics of an inactive syllabus subject cannot be changed");
        }
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(SyllabusTopicRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Syllabus topic request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateStatusRequest(SyllabusTopicStatusRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Syllabus topic status request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidRequestException(
                    "A valid syllabus target-date range of at most 367 days is required");
        }
    }

    private void validate(StudySyllabusTopic topic) {
        var violations = validator.validate(topic);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private StudySyllabusTopic save(StudySyllabusTopic topic) {
        try {
            return topics.save(topic);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateSyllabusTopicException(topic.getTitle(), exception);
        }
    }

    private String titleKey(String title) {
        return title.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SyllabusTopicResponse response(StudySyllabusTopic topic) {
        return new SyllabusTopicResponse(topic.getId(), topic.getSyllabusSubjectId(),
                topic.getTitle(), topic.getNote(), topic.getStatus(), topic.getDisplayOrder(),
                topic.getTargetDate(), topic.getCompletedAt(), topic.getCreatedAt(),
                topic.getUpdatedAt());
    }
}
