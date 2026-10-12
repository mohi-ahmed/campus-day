package com.spark.campusday.careerprep.question.controller;

import com.spark.campusday.careerprep.question.dto.QuestionImportRequest;
import com.spark.campusday.careerprep.question.dto.QuestionImportResponse;
import com.spark.campusday.careerprep.question.service.QuestionImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Internal development API. Protect this route before exposing the application publicly. */
@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
@Validated
@RestController
@RequestMapping("/api/internal/career-prep/question-imports")
@RequiredArgsConstructor
public class QuestionImportController {
    private final QuestionImportService service;

    @PostMapping
    public ResponseEntity<QuestionImportResponse> importQuestions(
            @Valid @RequestBody QuestionImportRequest request) {
        QuestionImportResponse imported = service.importQuestions(request);
        return ResponseEntity.created(URI.create(
                "/api/internal/career-prep/question-imports/" + imported.importBatchId()))
                .body(imported);
    }

    @GetMapping("/{batchId}")
    public QuestionImportResponse get(@PathVariable @NotBlank String batchId) {
        return service.getBatch(batchId);
    }
}
