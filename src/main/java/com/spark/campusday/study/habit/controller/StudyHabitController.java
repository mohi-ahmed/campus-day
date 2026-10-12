package com.spark.campusday.study.habit.controller;

import com.spark.campusday.study.habit.dto.HabitCompletionRequest;
import com.spark.campusday.study.habit.dto.HabitCompletionResponse;
import com.spark.campusday.study.habit.dto.StudyHabitProgressResponse;
import com.spark.campusday.study.habit.dto.StudyHabitRequest;
import com.spark.campusday.study.habit.dto.StudyHabitResponse;
import com.spark.campusday.study.habit.service.StudyHabitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/habits")
@RequiredArgsConstructor
public class StudyHabitController {
    private final StudyHabitService service;

    @PostMapping
    public ResponseEntity<StudyHabitResponse> create(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody StudyHabitRequest request) {
        StudyHabitResponse created = service.create(studentId, request);
        return ResponseEntity.created(URI.create("/api/study/habits/" + created.id()))
                .body(created);
    }

    @PutMapping("/{habitId}")
    public StudyHabitResponse update(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId,
            @Valid @RequestBody StudyHabitRequest request) {
        return service.update(studentId, habitId, request);
    }

    @GetMapping
    public List<StudyHabitResponse> list(
            @RequestParam @NotBlank String studentId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.list(studentId, activeOnly);
    }

    @GetMapping("/progress")
    public List<StudyHabitProgressResponse> progress(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.progress(studentId, from, to, activeOnly);
    }

    @GetMapping("/{habitId}")
    public StudyHabitResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId) {
        return service.get(studentId, habitId);
    }

    @PutMapping("/{habitId}/active")
    public StudyHabitResponse setActive(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId,
            @RequestParam boolean active) {
        return service.setActive(studentId, habitId, active);
    }

    @PostMapping("/{habitId}/completions")
    public ResponseEntity<HabitCompletionResponse> complete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId,
            @Valid @RequestBody HabitCompletionRequest request) {
        HabitCompletionResponse created = service.complete(studentId, habitId, request);
        return ResponseEntity.created(URI.create("/api/study/habits/" + habitId
                + "/completions/" + created.completedOn())).body(created);
    }

    @GetMapping("/{habitId}/completions")
    public List<HabitCompletionResponse> completions(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.completions(studentId, habitId, from, to);
    }

    @DeleteMapping("/{habitId}/completions/{completedOn}")
    public ResponseEntity<Void> uncomplete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedOn) {
        service.uncomplete(studentId, habitId, completedOn);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> delete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String habitId) {
        service.delete(studentId, habitId);
        return ResponseEntity.noContent().build();
    }
}
