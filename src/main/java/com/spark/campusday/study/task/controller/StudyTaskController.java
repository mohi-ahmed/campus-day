package com.spark.campusday.study.task.controller;

import com.spark.campusday.study.task.dto.StudyTaskRequest;
import com.spark.campusday.study.task.dto.StudyTaskResponse;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.service.StudyTaskService;
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
@RequestMapping("/api/study/tasks")
@RequiredArgsConstructor
public class StudyTaskController {
    private final StudyTaskService service;

    @PostMapping
    public ResponseEntity<StudyTaskResponse> create(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody StudyTaskRequest request) {
        StudyTaskResponse created = service.create(studentId, request);
        return ResponseEntity.created(URI.create("/api/study/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{taskId}")
    public StudyTaskResponse update(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String taskId,
            @Valid @RequestBody StudyTaskRequest request) {
        return service.update(studentId, taskId, request);
    }

    @GetMapping
    public List<StudyTaskResponse> list(@RequestParam @NotBlank String studentId) {
        return service.list(studentId);
    }

    @GetMapping("/by-status/{status}")
    public List<StudyTaskResponse> byStatus(
            @RequestParam @NotBlank String studentId,
            @PathVariable TaskStatus status) {
        return service.listByStatus(studentId, status);
    }

    @GetMapping("/between")
    public List<StudyTaskResponse> between(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.listBetween(studentId, from, to);
    }

    @GetMapping("/{taskId}")
    public StudyTaskResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String taskId) {
        return service.get(studentId, taskId);
    }

    @PostMapping("/{taskId}/complete")
    public StudyTaskResponse complete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String taskId) {
        return service.complete(studentId, taskId);
    }

    @PostMapping("/{taskId}/reopen")
    public StudyTaskResponse reopen(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String taskId) {
        return service.reopen(studentId, taskId);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String taskId) {
        service.delete(studentId, taskId);
        return ResponseEntity.noContent().build();
    }
}
