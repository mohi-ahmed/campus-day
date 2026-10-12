package com.spark.campusday.study.habit.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.habit.dto.HabitCompletionRequest;
import com.spark.campusday.study.habit.dto.HabitCompletionResponse;
import com.spark.campusday.study.habit.dto.StudyHabitProgressResponse;
import com.spark.campusday.study.habit.dto.StudyHabitRequest;
import com.spark.campusday.study.habit.dto.StudyHabitResponse;
import com.spark.campusday.study.habit.exception.DuplicateStudyHabitException;
import com.spark.campusday.study.habit.exception.HabitAlreadyCompletedException;
import com.spark.campusday.study.habit.exception.HabitCompletionNotFoundException;
import com.spark.campusday.study.habit.exception.InvalidStudyHabitStateException;
import com.spark.campusday.study.habit.exception.StudyHabitNotFoundException;
import com.spark.campusday.study.habit.model.StudyHabit;
import com.spark.campusday.study.habit.model.StudyHabitCompletion;
import com.spark.campusday.study.habit.repository.StudyHabitCompletionRepository;
import com.spark.campusday.study.habit.repository.StudyHabitRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyHabitService {
    private final StudyHabitRepository habits;
    private final StudyHabitCompletionRepository completions;
    private final StudentService students;
    private final Validator validator;

    public StudyHabitResponse create(String studentId, StudyHabitRequest request) {
        validateRequest(request);
        String ownerId = ownerId(studentId);
        String nameKey = nameKey(request.name());
        if (habits.existsByStudentIdAndNameKey(ownerId, nameKey)) {
            throw new DuplicateStudyHabitException(request.name().trim());
        }

        StudyHabit habit = StudyHabit.builder()
                .studentId(ownerId)
                .name(request.name().trim())
                .nameKey(nameKey)
                .description(normalize(request.description()))
                .repeatDays(Set.copyOf(request.repeatDays()))
                .displayOrder(request.displayOrder())
                .active(true)
                .build();

        validate(habit);
        return response(save(habit));
    }

    public StudyHabitResponse update(String studentId, String habitId,
                                     StudyHabitRequest request) {
        validateRequest(request);
        StudyHabit habit = requireHabit(studentId, habitId);
        String nameKey = nameKey(request.name());
        if (habits.existsByStudentIdAndNameKeyAndIdNot(
                habit.getStudentId(), nameKey, habit.getId())) {
            throw new DuplicateStudyHabitException(request.name().trim());
        }

        habit.setName(request.name().trim());
        habit.setNameKey(nameKey);
        habit.setDescription(normalize(request.description()));
        habit.setRepeatDays(Set.copyOf(request.repeatDays()));
        habit.setDisplayOrder(request.displayOrder());

        validate(habit);
        return response(save(habit));
    }

    public StudyHabitResponse get(String studentId, String habitId) {
        return response(requireHabit(studentId, habitId));
    }

    public List<StudyHabitResponse> list(String studentId, boolean activeOnly) {
        String ownerId = ownerId(studentId);
        List<StudyHabit> result = activeOnly
                ? habits.findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId)
                : habits.findByStudentIdOrderByDisplayOrderAscNameAsc(ownerId);
        return result.stream().map(this::response).toList();
    }

    public List<StudyHabitProgressResponse> progress(String studentId, LocalDate from,
                                                     LocalDate to, boolean activeOnly) {
        validateRange(from, to);
        String ownerId = ownerId(studentId);
        List<StudyHabit> result = activeOnly
                ? habits.findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId)
                : habits.findByStudentIdOrderByDisplayOrderAscNameAsc(ownerId);
        Map<String, Set<LocalDate>> datesByHabit = completions
                .findByStudentIdAndCompletedOnGreaterThanEqualAndCompletedOnLessThanEqualOrderByCompletedOnAsc(
                        ownerId, from, to)
                .stream()
                .collect(Collectors.groupingBy(StudyHabitCompletion::getHabitId,
                        Collectors.mapping(StudyHabitCompletion::getCompletedOn, Collectors.toSet())));

        return result.stream()
                .map(habit -> progressResponse(habit,
                        datesByHabit.getOrDefault(habit.getId(), Collections.emptySet())))
                .toList();
    }

    public StudyHabitResponse setActive(String studentId, String habitId, boolean active) {
        StudyHabit habit = requireHabit(studentId, habitId);
        if (habit.isActive() == active) {
            throw new InvalidStudyHabitStateException(
                    active ? "Study habit is already active" : "Study habit is already inactive");
        }
        habit.setActive(active);
        return response(habits.save(habit));
    }

    public HabitCompletionResponse complete(String studentId, String habitId,
                                              HabitCompletionRequest request) {
        validateCompletionRequest(request);
        StudyHabit habit = requireHabit(studentId, habitId);
        LocalDate completedOn = request.completedOn();
        validateCompletionDate(habit, completedOn);
        if (completions.existsByHabitIdAndStudentIdAndCompletedOn(
                habit.getId(), habit.getStudentId(), completedOn)) {
            throw new HabitAlreadyCompletedException(habit.getId(), completedOn);
        }

        StudyHabitCompletion completion = StudyHabitCompletion.builder()
                .studentId(habit.getStudentId())
                .habitId(habit.getId())
                .completedOn(completedOn)
                .build();
        validate(completion);

        try {
            return completionResponse(completions.save(completion));
        } catch (DuplicateKeyException exception) {
            throw new HabitAlreadyCompletedException(habit.getId(), completedOn, exception);
        }
    }

    public void uncomplete(String studentId, String habitId, LocalDate completedOn) {
        StudyHabit habit = requireHabit(studentId, habitId);
        if (completedOn == null) {
            throw new InvalidRequestException("Habit completion date is required");
        }
        long deleted = completions.deleteByHabitIdAndStudentIdAndCompletedOn(
                habit.getId(), habit.getStudentId(), completedOn);
        if (deleted == 0) {
            throw new HabitCompletionNotFoundException(habit.getId(), completedOn);
        }
    }

    public List<HabitCompletionResponse> completions(String studentId, String habitId,
                                                     LocalDate from, LocalDate to) {
        validateRange(from, to);
        StudyHabit habit = requireHabit(studentId, habitId);
        return completions
                .findByHabitIdAndStudentIdAndCompletedOnGreaterThanEqualAndCompletedOnLessThanEqualOrderByCompletedOnAsc(
                        habit.getId(), habit.getStudentId(), from, to)
                .stream().map(this::completionResponse).toList();
    }

    public void delete(String studentId, String habitId) {
        StudyHabit habit = requireHabit(studentId, habitId);
        completions.deleteByHabitIdAndStudentId(habit.getId(), habit.getStudentId());
        habits.delete(habit);
    }

    private StudyHabit requireHabit(String studentId, String habitId) {
        return habits.findByIdAndStudentId(habitId, ownerId(studentId))
                .orElseThrow(() -> new StudyHabitNotFoundException(habitId));
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(StudyHabitRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Study habit request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateCompletionRequest(HabitCompletionRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Habit completion request is required");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateCompletionDate(StudyHabit habit, LocalDate completedOn) {
        if (!habit.isActive()) {
            throw new InvalidStudyHabitStateException("Inactive study habit cannot be completed");
        }
        if (completedOn.isAfter(LocalDate.now())) {
            throw new InvalidStudyHabitStateException("A future habit date cannot be completed");
        }
        if (!habit.getRepeatDays().contains(completedOn.getDayOfWeek())) {
            throw new InvalidStudyHabitStateException(
                    "Study habit is not scheduled for " + completedOn.getDayOfWeek());
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidRequestException("A valid habit date range of at most 367 days is required");
        }
    }

    private void validate(Object value) {
        var violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private StudyHabit save(StudyHabit habit) {
        try {
            return habits.save(habit);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateStudyHabitException(habit.getName(), exception);
        }
    }

    private String nameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StudyHabitResponse response(StudyHabit habit) {
        return new StudyHabitResponse(habit.getId(), habit.getName(), habit.getDescription(),
                habit.getRepeatDays(), habit.getDisplayOrder(), habit.isActive(),
                habit.getCreatedAt(), habit.getUpdatedAt());
    }

    private HabitCompletionResponse completionResponse(StudyHabitCompletion completion) {
        return new HabitCompletionResponse(completion.getId(), completion.getHabitId(),
                completion.getCompletedOn(), completion.getCreatedAt());
    }

    private StudyHabitProgressResponse progressResponse(StudyHabit habit,
                                                        Set<LocalDate> completedDates) {
        return new StudyHabitProgressResponse(habit.getId(), habit.getName(),
                habit.getDescription(), habit.getRepeatDays(), habit.getDisplayOrder(),
                habit.isActive(), Set.copyOf(completedDates), habit.getCreatedAt(),
                habit.getUpdatedAt());
    }
}
