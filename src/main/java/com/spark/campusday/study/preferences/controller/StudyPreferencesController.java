package com.spark.campusday.study.preferences.controller;

import com.spark.campusday.study.preferences.dto.StudyPreferencesRequest;
import com.spark.campusday.study.preferences.dto.StudyPreferencesResponse;
import com.spark.campusday.study.preferences.service.StudyPreferencesService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/preferences")
@RequiredArgsConstructor
public class StudyPreferencesController {
    private final StudyPreferencesService service;

    @GetMapping
    public StudyPreferencesResponse get(@RequestParam @NotBlank String studentId) {
        return service.get(studentId);
    }

    @PutMapping
    public StudyPreferencesResponse update(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody StudyPreferencesRequest request) {
        return service.update(studentId, request);
    }

    @PostMapping("/reset")
    public StudyPreferencesResponse reset(@RequestParam @NotBlank String studentId) {
        return service.reset(studentId);
    }
}
