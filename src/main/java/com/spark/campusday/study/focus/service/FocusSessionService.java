package com.spark.campusday.study.focus.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.focus.dto.FocusSessionRequest;
import com.spark.campusday.study.focus.dto.FocusSessionResponse;
import com.spark.campusday.study.focus.exception.FocusSessionNotFoundException;
import com.spark.campusday.study.focus.exception.InvalidFocusSessionStateException;
import com.spark.campusday.study.focus.model.FocusSession;
import com.spark.campusday.study.focus.model.FocusSessionOutcome;
import com.spark.campusday.study.focus.repository.FocusSessionRepository;
import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.exception.StudyTaskNotFoundException;
import com.spark.campusday.study.task.repository.StudyTaskRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FocusSessionService {
    private final FocusSessionRepository sessions;
    private final StudyTaskRepository tasks;
    private final StudentService students;
    private final Validator validator;

    public FocusSessionResponse record(String studentId, FocusSessionRequest request) {
        validateRequest(request);
        String ownerId = ownerId(studentId);
        StudyTask task = tasks.findByIdAndStudentId(request.taskId(), ownerId)
                .orElseThrow(() -> new StudyTaskNotFoundException(request.taskId()));

        FocusSession session = FocusSession.builder()
                .studentId(ownerId)
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .subject(task.getSubject())
                .focusDate(request.focusDate())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .plannedMinutes(request.plannedMinutes())
                .focusedMinutes(request.focusedMinutes())
                .outcome(request.outcome())
                .build();

        validate(session);
        return response(sessions.save(session));
    }

    public FocusSessionResponse get(String studentId, String sessionId) {
        return response(requireSession(studentId, sessionId));
    }

    public List<FocusSessionResponse> listForDate(String studentId, LocalDate focusDate) {
        if (focusDate == null) {
            throw new InvalidRequestException("Focus date is required");
        }
        return sessions.findByStudentIdAndFocusDateOrderByStartedAtDesc(
                        ownerId(studentId), focusDate)
                .stream().map(this::response).toList();
    }

    public List<FocusSessionResponse> listBetween(String studentId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return sessions.findByStudentIdAndFocusDateGreaterThanEqualAndFocusDateLessThanEqualOrderByStartedAtDesc(
                        ownerId(studentId), from, to)
                .stream().map(this::response).toList();
    }

    public List<FocusSessionResponse> listForTask(String studentId, String taskId) {
        String ownerId = ownerId(studentId);
        if (!tasks.existsByIdAndStudentId(taskId, ownerId)) {
            throw new StudyTaskNotFoundException(taskId);
        }
        return sessions.findByStudentIdAndTaskIdOrderByStartedAtDesc(ownerId, taskId)
                .stream().map(this::response).toList();
    }

    public void delete(String studentId, String sessionId) {
        sessions.delete(requireSession(studentId, sessionId));
    }

    private FocusSession requireSession(String studentId, String sessionId) {
        return sessions.findByIdAndStudentId(sessionId, ownerId(studentId))
                .orElseThrow(() -> new FocusSessionNotFoundException(sessionId));
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(FocusSessionRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Focus session request is required");
        }
        if (request.taskId() == null || request.taskId().isBlank()
                || request.focusDate() == null || request.startedAt() == null
                || request.endedAt() == null || request.outcome() == null) {
            throw new InvalidRequestException("Task, focus date, times and outcome are required");
        }
        if (!request.startedAt().isBefore(request.endedAt())) {
            throw new InvalidFocusSessionStateException(
                    "Focus session start time must be before end time");
        }
        if (request.plannedMinutes() < 5 || request.plannedMinutes() > 180
                || request.focusedMinutes() < 1
                || request.focusedMinutes() > request.plannedMinutes()) {
            throw new InvalidFocusSessionStateException("Focus duration is outside the supported range");
        }
        if (request.outcome() == FocusSessionOutcome.COMPLETED
                && request.focusedMinutes() != request.plannedMinutes()) {
            throw new InvalidFocusSessionStateException(
                    "A completed focus session must reach its planned duration");
        }
        long elapsedMinutes = Duration.between(request.startedAt(), request.endedAt()).toMinutes();
        if (elapsedMinutes < request.focusedMinutes()) {
            throw new InvalidFocusSessionStateException(
                    "Focused minutes cannot exceed the elapsed session time");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidRequestException("A valid focus date range of at most 367 days is required");
        }
    }

    private void validate(FocusSession session) {
        var violations = validator.validate(session);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private FocusSessionResponse response(FocusSession session) {
        return new FocusSessionResponse(session.getId(), session.getTaskId(),
                session.getTaskTitle(), session.getSubject(), session.getFocusDate(),
                session.getStartedAt(), session.getEndedAt(), session.getPlannedMinutes(),
                session.getFocusedMinutes(), session.getOutcome(), session.getCreatedAt());
    }
}
