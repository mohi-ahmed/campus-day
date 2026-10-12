package com.spark.campusday.study.routine.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.integration.service.StudyCalendarAutoSyncService;
import com.spark.campusday.study.routine.dto.StudyRoutineRequest;
import com.spark.campusday.study.routine.dto.StudyRoutineResponse;
import com.spark.campusday.study.routine.exception.InvalidStudyRoutineStateException;
import com.spark.campusday.study.routine.exception.StudyRoutineNotFoundException;
import com.spark.campusday.study.routine.model.StudyRoutineBlock;
import com.spark.campusday.study.routine.repository.StudyRoutineRepository;
import com.spark.campusday.study.reminder.service.StudyReminderSchedulingService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyRoutineService {
    private final StudyRoutineRepository routines;
    private final StudentService students;
    private final Validator validator;
    private final StudyReminderSchedulingService reminderScheduling;
    private final StudyCalendarAutoSyncService calendarAutoSync;

    public StudyRoutineResponse create(String studentId, StudyRoutineRequest request) {
        validateRequest(request);
        StudyRoutineBlock block = StudyRoutineBlock.builder()
                .studentId(ownerId(studentId))
                .subject(request.subject().trim())
                .blockType(request.blockType().trim())
                .topic(request.topic().trim())
                .location(normalize(request.location()))
                .days(Set.copyOf(request.days()))
                .startTime(request.startTime())
                .endTime(request.endTime())
                .active(true)
                .build();

        validate(block);
        StudyRoutineBlock saved = routines.save(block);
        reminderScheduling.syncRoutineSafely(saved);
        calendarAutoSync.syncHorizonSafely(studentId);
        return response(saved);
    }

    public StudyRoutineResponse update(String studentId, String blockId,
                                       StudyRoutineRequest request) {
        validateRequest(request);
        StudyRoutineBlock block = requireBlock(studentId, blockId);
        block.setSubject(request.subject().trim());
        block.setBlockType(request.blockType().trim());
        block.setTopic(request.topic().trim());
        block.setLocation(normalize(request.location()));
        block.setDays(Set.copyOf(request.days()));
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());

        validate(block);
        StudyRoutineBlock saved = routines.save(block);
        reminderScheduling.syncRoutineSafely(saved);
        calendarAutoSync.syncHorizonSafely(studentId);
        return response(saved);
    }

    public StudyRoutineResponse get(String studentId, String blockId) {
        return response(requireBlock(studentId, blockId));
    }

    public List<StudyRoutineResponse> list(String studentId, boolean activeOnly) {
        String ownerId = ownerId(studentId);
        List<StudyRoutineBlock> blocks = activeOnly
                ? routines.findByStudentIdAndActiveTrueOrderByStartTimeAsc(ownerId)
                : routines.findByStudentIdOrderByStartTimeAsc(ownerId);
        return blocks.stream().map(this::response).toList();
    }

    public List<StudyRoutineResponse> listForDay(String studentId, DayOfWeek day) {
        if (day == null) {
            throw new InvalidRequestException("Routine day is required");
        }
        return routines.findByStudentIdAndActiveTrueAndDaysContainingOrderByStartTimeAsc(
                        ownerId(studentId), day)
                .stream().map(this::response).toList();
    }

    public StudyRoutineResponse setActive(String studentId, String blockId, boolean active) {
        StudyRoutineBlock block = requireBlock(studentId, blockId);
        if (block.isActive() == active) {
            throw new InvalidStudyRoutineStateException(
                    active ? "Routine block is already active" : "Routine block is already inactive");
        }
        block.setActive(active);
        StudyRoutineBlock saved = routines.save(block);
        reminderScheduling.syncRoutineSafely(saved);
        calendarAutoSync.syncHorizonSafely(studentId);
        return response(saved);
    }

    public void delete(String studentId, String blockId) {
        StudyRoutineBlock block = requireBlock(studentId, blockId);
        reminderScheduling.cancelRoutineSafely(block);
        routines.delete(block);
        calendarAutoSync.syncHorizonSafely(studentId);
    }

    private StudyRoutineBlock requireBlock(String studentId, String blockId) {
        return routines.findByIdAndStudentId(blockId, ownerId(studentId))
                .orElseThrow(() -> new StudyRoutineNotFoundException(blockId));
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(StudyRoutineRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Study routine request is required");
        }
        if (request.subject() == null || request.subject().isBlank()
                || request.blockType() == null || request.blockType().isBlank()
                || request.topic() == null || request.topic().isBlank()) {
            throw new InvalidRequestException("Subject, block type and topic are required");
        }
        if (request.days() == null || request.days().isEmpty()
                || request.days().stream().anyMatch(day -> day == null)) {
            throw new InvalidRequestException("At least one repeat day is required");
        }
        if (request.startTime() == null || request.endTime() == null
                || !request.startTime().isBefore(request.endTime())) {
            throw new InvalidStudyRoutineStateException(
                    "Routine start time must be before end time");
        }
    }

    private void validate(StudyRoutineBlock block) {
        var violations = validator.validate(block);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StudyRoutineResponse response(StudyRoutineBlock block) {
        return new StudyRoutineResponse(block.getId(), block.getSubject(), block.getBlockType(),
                block.getTopic(), block.getLocation(), block.getDays(), block.getStartTime(),
                block.getEndTime(), block.isActive(), block.getCreatedAt(), block.getUpdatedAt());
    }
}
