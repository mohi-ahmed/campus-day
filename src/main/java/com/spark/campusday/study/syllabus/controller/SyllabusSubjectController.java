package com.spark.campusday.study.syllabus.controller;

import com.spark.campusday.study.syllabus.dto.SyllabusSubjectProgressResponse;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectRequest;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectResponse;
import com.spark.campusday.study.syllabus.service.SyllabusSubjectService;
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
import java.util.List;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/syllabus/subjects")
@RequiredArgsConstructor
public class SyllabusSubjectController {
    private final SyllabusSubjectService service;

    @PostMapping
    public ResponseEntity<SyllabusSubjectResponse> create(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody SyllabusSubjectRequest request) {
        SyllabusSubjectResponse created = service.create(studentId, request);
        return ResponseEntity.created(URI.create(
                "/api/study/syllabus/subjects/" + created.id())).body(created);
    }

    @PutMapping("/{subjectId}")
    public SyllabusSubjectResponse update(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @Valid @RequestBody SyllabusSubjectRequest request) {
        return service.update(studentId, subjectId, request);
    }

    @GetMapping
    public List<SyllabusSubjectResponse> list(
            @RequestParam @NotBlank String studentId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.list(studentId, activeOnly);
    }

    @GetMapping("/progress")
    public List<SyllabusSubjectProgressResponse> progressList(
            @RequestParam @NotBlank String studentId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.progressList(studentId, activeOnly);
    }

    @GetMapping("/{subjectId}")
    public SyllabusSubjectResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId) {
        return service.get(studentId, subjectId);
    }

    @GetMapping("/{subjectId}/progress")
    public SyllabusSubjectProgressResponse progress(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId) {
        return service.progress(studentId, subjectId);
    }

    @PutMapping("/{subjectId}/active")
    public SyllabusSubjectResponse setActive(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @RequestParam boolean active) {
        return service.setActive(studentId, subjectId, active);
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Void> delete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId) {
        service.delete(studentId, subjectId);
        return ResponseEntity.noContent().build();
    }
}
