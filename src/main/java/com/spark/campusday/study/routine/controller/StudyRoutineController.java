package com.spark.campusday.study.routine.controller;

import com.spark.campusday.study.routine.dto.StudyRoutineRequest;
import com.spark.campusday.study.routine.dto.StudyRoutineResponse;
import com.spark.campusday.study.routine.service.StudyRoutineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.DayOfWeek;
import java.util.List;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/routines")
@RequiredArgsConstructor
public class StudyRoutineController {
    private final StudyRoutineService service;

    @PostMapping
    public ResponseEntity<StudyRoutineResponse> create(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody StudyRoutineRequest request) {
        StudyRoutineResponse created = service.create(studentId, request);
        return ResponseEntity.created(URI.create("/api/study/routines/" + created.id()))
                .body(created);
    }

    @PutMapping("/{blockId}")
    public StudyRoutineResponse update(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String blockId,
            @Valid @RequestBody StudyRoutineRequest request) {
        return service.update(studentId, blockId, request);
    }

    @GetMapping
    public List<StudyRoutineResponse> list(
            @RequestParam @NotBlank String studentId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.list(studentId, activeOnly);
    }

    @GetMapping("/by-day/{day}")
    public List<StudyRoutineResponse> byDay(
            @RequestParam @NotBlank String studentId,
            @PathVariable DayOfWeek day) {
        return service.listForDay(studentId, day);
    }

    @GetMapping("/{blockId}")
    public StudyRoutineResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String blockId) {
        return service.get(studentId, blockId);
    }

    @PutMapping("/{blockId}/active")
    public StudyRoutineResponse setActive(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String blockId,
            @RequestParam boolean active) {
        return service.setActive(studentId, blockId, active);
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> delete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String blockId) {
        service.delete(studentId, blockId);
        return ResponseEntity.noContent().build();
    }
}
