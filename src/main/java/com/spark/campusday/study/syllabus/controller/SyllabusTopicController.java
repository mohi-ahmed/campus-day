package com.spark.campusday.study.syllabus.controller;

import com.spark.campusday.study.syllabus.dto.SyllabusTopicRequest;
import com.spark.campusday.study.syllabus.dto.SyllabusTopicResponse;
import com.spark.campusday.study.syllabus.dto.SyllabusTopicStatusRequest;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.service.SyllabusTopicService;
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
@RequestMapping("/api/study/syllabus")
@RequiredArgsConstructor
public class SyllabusTopicController {
    private final SyllabusTopicService service;

    @PostMapping("/subjects/{subjectId}/topics")
    public ResponseEntity<SyllabusTopicResponse> create(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @Valid @RequestBody SyllabusTopicRequest request) {
        SyllabusTopicResponse created = service.create(studentId, subjectId, request);
        return ResponseEntity.created(URI.create("/api/study/syllabus/subjects/"
                + subjectId + "/topics/" + created.id())).body(created);
    }

    @PutMapping("/subjects/{subjectId}/topics/{topicId}")
    public SyllabusTopicResponse update(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @PathVariable @NotBlank String topicId,
            @Valid @RequestBody SyllabusTopicRequest request) {
        return service.update(studentId, subjectId, topicId, request);
    }

    @GetMapping("/subjects/{subjectId}/topics")
    public List<SyllabusTopicResponse> list(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId) {
        return service.list(studentId, subjectId);
    }

    @GetMapping("/subjects/{subjectId}/topics/{topicId}")
    public SyllabusTopicResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @PathVariable @NotBlank String topicId) {
        return service.get(studentId, subjectId, topicId);
    }

    @PutMapping("/subjects/{subjectId}/topics/{topicId}/status")
    public SyllabusTopicResponse updateStatus(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @PathVariable @NotBlank String topicId,
            @Valid @RequestBody SyllabusTopicStatusRequest request) {
        return service.updateStatus(studentId, subjectId, topicId, request);
    }

    @GetMapping("/topics/by-status/{status}")
    public List<SyllabusTopicResponse> byStatus(
            @RequestParam @NotBlank String studentId,
            @PathVariable SyllabusTopicStatus status) {
        return service.listByStatus(studentId, status);
    }

    @GetMapping("/topics/by-target-date")
    public List<SyllabusTopicResponse> byTargetDate(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.listByTargetDate(studentId, from, to);
    }

    @DeleteMapping("/subjects/{subjectId}/topics/{topicId}")
    public ResponseEntity<Void> delete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String subjectId,
            @PathVariable @NotBlank String topicId) {
        service.delete(studentId, subjectId, topicId);
        return ResponseEntity.noContent().build();
    }
}
