package com.spark.campusday.study.task.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.integration.service.StudyCalendarAutoSyncService;
import com.spark.campusday.study.task.dto.StudyTaskRequest;
import com.spark.campusday.study.task.dto.StudyTaskResponse;
import com.spark.campusday.study.task.exception.InvalidStudyTaskStateException;
import com.spark.campusday.study.task.exception.StudyTaskNotFoundException;
import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.repository.StudyTaskRepository;
import com.spark.campusday.study.reminder.service.StudyReminderSchedulingService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyTaskService {
    private final StudyTaskRepository tasks;
    private final StudentService students;
    private final Validator validator;
    private final StudyReminderSchedulingService reminderScheduling;
    private final StudyCalendarAutoSyncService calendarAutoSync;

    public StudyTaskResponse create(String studentId, StudyTaskRequest request) {
        Student student = students.getByStudentId(studentId);
        validateRequest(request);

        StudyTask task = StudyTask.builder()
                .studentId(student.getId())
                .title(request.title().trim())
                .subject(request.subject().trim())
                .taskType(request.taskType().trim())
                .priority(request.priority())
                .dueDate(request.dueDate())
                .dueTime(request.dueTime())
                .estimatedMinutes(request.estimatedMinutes())
                .repeat(request.repeat())
                .note(normalize(request.note()))
                .status(TaskStatus.PENDING)
                .build();

        validate(task);
        StudyTask saved = tasks.save(task);
        reminderScheduling.syncTaskSafely(saved);
        calendarAutoSync.syncDatesSafely(studentId, saved.getDueDate());
        return response(saved);
    }

    public StudyTaskResponse update(String studentId, String taskId, StudyTaskRequest request) {
        validateRequest(request);
        StudyTask task = requireTask(studentId, taskId);
        LocalDate previousDueDate = task.getDueDate();

        task.setTitle(request.title().trim());
        task.setSubject(request.subject().trim());
        task.setTaskType(request.taskType().trim());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setDueTime(request.dueTime());
        task.setEstimatedMinutes(request.estimatedMinutes());
        task.setRepeat(request.repeat());
        task.setNote(normalize(request.note()));

        validate(task);
        StudyTask saved = tasks.save(task);
        reminderScheduling.syncTaskSafely(saved);
        calendarAutoSync.syncDatesSafely(
                studentId, previousDueDate, saved.getDueDate());
        return response(saved);
    }

    public StudyTaskResponse get(String studentId, String taskId) {
        return response(requireTask(studentId, taskId));
    }

    public List<StudyTaskResponse> list(String studentId) {
        String ownerId = ownerId(studentId);
        return tasks.findByStudentIdOrderByDueDateAscDueTimeAsc(ownerId)
                .stream().map(this::response).toList();
    }

    public List<StudyTaskResponse> listByStatus(String studentId, TaskStatus status) {
        if (status == null) {
            throw new InvalidRequestException("Task status is required");
        }
        String ownerId = ownerId(studentId);
        return tasks.findByStudentIdAndStatusOrderByDueDateAscDueTimeAsc(ownerId, status)
                .stream().map(this::response).toList();
    }

    public List<StudyTaskResponse> listBetween(String studentId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        String ownerId = ownerId(studentId);
        return tasks.findByStudentIdAndDueDateGreaterThanEqualAndDueDateLessThanEqualOrderByDueDateAscDueTimeAsc(
                        ownerId, from, to)
                .stream().map(this::response).toList();
    }

    public StudyTaskResponse complete(String studentId, String taskId) {
        StudyTask task = requireTask(studentId, taskId);
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new InvalidStudyTaskStateException("Task is already completed");
        }
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        StudyTask saved = tasks.save(task);
        reminderScheduling.syncTaskSafely(saved);
        calendarAutoSync.syncDatesSafely(studentId, saved.getDueDate());
        return response(saved);
    }

    public StudyTaskResponse reopen(String studentId, String taskId) {
        StudyTask task = requireTask(studentId, taskId);
        if (task.getStatus() == TaskStatus.PENDING) {
            throw new InvalidStudyTaskStateException("Task is already pending");
        }
        task.setStatus(TaskStatus.PENDING);
        task.setCompletedAt(null);
        StudyTask saved = tasks.save(task);
        reminderScheduling.syncTaskSafely(saved);
        calendarAutoSync.syncDatesSafely(studentId, saved.getDueDate());
        return response(saved);
    }

    public void delete(String studentId, String taskId) {
        StudyTask task = requireTask(studentId, taskId);
        reminderScheduling.cancelTaskSafely(task);
        tasks.delete(task);
        calendarAutoSync.syncDatesSafely(studentId, task.getDueDate());
    }

    private StudyTask requireTask(String studentId, String taskId) {
        return tasks.findByIdAndStudentId(taskId, ownerId(studentId))
                .orElseThrow(() -> new StudyTaskNotFoundException(taskId));
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(StudyTaskRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Study task request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidRequestException("A valid task date range of at most 367 days is required");
        }
    }

    private void validate(StudyTask task) {
        var violations = validator.validate(task);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StudyTaskResponse response(StudyTask task) {
        return new StudyTaskResponse(task.getId(), task.getTitle(), task.getSubject(),
                task.getTaskType(), task.getPriority(), task.getDueDate(), task.getDueTime(),
                task.getEstimatedMinutes(), task.getRepeat(), task.getNote(), task.getStatus(),
                task.getCompletedAt(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
