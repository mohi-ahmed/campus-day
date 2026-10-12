package com.spark.campusday.study.syllabus.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.integration.service.StudyCalendarAutoSyncService;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectProgressResponse;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectRequest;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectResponse;
import com.spark.campusday.study.syllabus.dto.SyllabusTopicResponse;
import com.spark.campusday.study.syllabus.exception.DuplicateSyllabusSubjectException;
import com.spark.campusday.study.syllabus.exception.InvalidSyllabusStateException;
import com.spark.campusday.study.syllabus.exception.SyllabusSubjectNotFoundException;
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusSubjectService {
    private final StudySyllabusSubjectRepository subjects;
    private final StudySyllabusTopicRepository topics;
    private final StudentService students;
    private final Validator validator;
    private final StudyReminderSchedulingService reminderScheduling;
    private final StudyCalendarAutoSyncService calendarAutoSync;

    public SyllabusSubjectResponse create(String studentId, SyllabusSubjectRequest request) {
        validateRequest(request);
        String ownerId = ownerId(studentId);
        String nameKey = nameKey(request.name());
        if (subjects.existsByStudentIdAndNameKey(ownerId, nameKey)) {
            throw new DuplicateSyllabusSubjectException(request.name().trim());
        }

        StudySyllabusSubject subject = StudySyllabusSubject.builder()
                .studentId(ownerId)
                .name(request.name().trim())
                .nameKey(nameKey)
                .description(normalize(request.description()))
                .color(normalizeColor(request.color()))
                .displayOrder(request.displayOrder())
                .active(true)
                .build();
        validate(subject);
        return response(save(subject));
    }

    public SyllabusSubjectResponse update(String studentId, String subjectId,
                                          SyllabusSubjectRequest request) {
        validateRequest(request);
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        String nameKey = nameKey(request.name());
        if (subjects.existsByStudentIdAndNameKeyAndIdNot(
                subject.getStudentId(), nameKey, subject.getId())) {
            throw new DuplicateSyllabusSubjectException(request.name().trim());
        }

        subject.setName(request.name().trim());
        subject.setNameKey(nameKey);
        subject.setDescription(normalize(request.description()));
        subject.setColor(normalizeColor(request.color()));
        subject.setDisplayOrder(request.displayOrder());
        validate(subject);
        StudySyllabusSubject saved = save(subject);
        calendarAutoSync.syncDatesSafely(studentId, targetDates(saved));
        return response(saved);
    }

    public SyllabusSubjectResponse get(String studentId, String subjectId) {
        return response(requireSubject(studentId, subjectId));
    }

    public List<SyllabusSubjectResponse> list(String studentId, boolean activeOnly) {
        String ownerId = ownerId(studentId);
        List<StudySyllabusSubject> result = activeOnly
                ? subjects.findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId)
                : subjects.findByStudentIdOrderByDisplayOrderAscNameAsc(ownerId);
        return result.stream().map(this::response).toList();
    }

    public SyllabusSubjectProgressResponse progress(String studentId, String subjectId) {
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        List<StudySyllabusTopic> subjectTopics = topics
                .findBySyllabusSubjectIdAndStudentIdOrderByDisplayOrderAscTitleAsc(
                        subject.getId(), subject.getStudentId());
        return progressResponse(subject, subjectTopics);
    }

    public List<SyllabusSubjectProgressResponse> progressList(String studentId,
                                                              boolean activeOnly) {
        String ownerId = ownerId(studentId);
        List<StudySyllabusSubject> result = activeOnly
                ? subjects.findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId)
                : subjects.findByStudentIdOrderByDisplayOrderAscNameAsc(ownerId);
        Map<String, List<StudySyllabusTopic>> topicsBySubject = topics
                .findByStudentIdOrderBySyllabusSubjectIdAscDisplayOrderAscTitleAsc(ownerId)
                .stream().collect(Collectors.groupingBy(StudySyllabusTopic::getSyllabusSubjectId));

        return result.stream()
                .map(subject -> progressResponse(subject,
                        topicsBySubject.getOrDefault(subject.getId(), Collections.emptyList())))
                .toList();
    }

    public SyllabusSubjectResponse setActive(String studentId, String subjectId, boolean active) {
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        if (subject.isActive() == active) {
            throw new InvalidSyllabusStateException(
                    active ? "Syllabus subject is already active" : "Syllabus subject is already inactive");
        }
        subject.setActive(active);
        StudySyllabusSubject saved = subjects.save(subject);
        topics.findBySyllabusSubjectIdAndStudentIdOrderByDisplayOrderAscTitleAsc(
                        saved.getId(), saved.getStudentId())
                .forEach(reminderScheduling::syncSyllabusTopicSafely);
        return response(saved);
    }

    public void delete(String studentId, String subjectId) {
        StudySyllabusSubject subject = requireSubject(studentId, subjectId);
        List<StudySyllabusTopic> subjectTopics = topics
                .findBySyllabusSubjectIdAndStudentIdOrderByDisplayOrderAscTitleAsc(
                        subject.getId(), subject.getStudentId());
        subjectTopics.forEach(reminderScheduling::cancelSyllabusTopicSafely);
        topics.deleteBySyllabusSubjectIdAndStudentId(subject.getId(), subject.getStudentId());
        subjects.delete(subject);
        calendarAutoSync.syncDatesSafely(studentId, subjectTopics.stream()
                .map(StudySyllabusTopic::getTargetDate)
                .filter(java.util.Objects::nonNull)
                .toArray(LocalDate[]::new));
    }

    private StudySyllabusSubject requireSubject(String studentId, String subjectId) {
        return subjects.findByIdAndStudentId(subjectId, ownerId(studentId))
                .orElseThrow(() -> new SyllabusSubjectNotFoundException(subjectId));
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(SyllabusSubjectRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Syllabus subject request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(StudySyllabusSubject subject) {
        var violations = validator.validate(subject);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private StudySyllabusSubject save(StudySyllabusSubject subject) {
        try {
            return subjects.save(subject);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateSyllabusSubjectException(subject.getName(), exception);
        }
    }

    private String nameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeColor(String color) {
        return color == null || color.isBlank() ? null : color.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate[] targetDates(StudySyllabusSubject subject) {
        return topics.findBySyllabusSubjectIdAndStudentIdOrderByDisplayOrderAscTitleAsc(
                        subject.getId(), subject.getStudentId())
                .stream()
                .map(StudySyllabusTopic::getTargetDate)
                .filter(java.util.Objects::nonNull)
                .toArray(LocalDate[]::new);
    }

    private SyllabusSubjectResponse response(StudySyllabusSubject subject) {
        return new SyllabusSubjectResponse(subject.getId(), subject.getName(),
                subject.getDescription(), subject.getColor(), subject.getDisplayOrder(),
                subject.isActive(), subject.getCreatedAt(), subject.getUpdatedAt());
    }

    private SyllabusTopicResponse topicResponse(StudySyllabusTopic topic) {
        return new SyllabusTopicResponse(topic.getId(), topic.getSyllabusSubjectId(),
                topic.getTitle(), topic.getNote(), topic.getStatus(), topic.getDisplayOrder(),
                topic.getTargetDate(), topic.getCompletedAt(), topic.getCreatedAt(),
                topic.getUpdatedAt());
    }

    private SyllabusSubjectProgressResponse progressResponse(
            StudySyllabusSubject subject, List<StudySyllabusTopic> subjectTopics) {
        int completed = (int) subjectTopics.stream()
                .filter(topic -> topic.getStatus() == SyllabusTopicStatus.COMPLETED)
                .count();
        return new SyllabusSubjectProgressResponse(response(subject), completed,
                subjectTopics.size(), subjectTopics.stream().map(this::topicResponse).toList());
    }
}
