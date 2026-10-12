package com.spark.campusday.careerprep.question.controller;

import com.spark.campusday.careerprep.question.dto.QuestionReviewRequest;
import com.spark.campusday.careerprep.question.dto.QuestionReviewResponse;
import com.spark.campusday.careerprep.question.service.QuestionReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal development API. Protect this route before exposing the application publicly. */
@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
@Validated
@RestController
@RequestMapping("/api/internal/career-prep/questions")
@RequiredArgsConstructor
public class QuestionReviewController {
    private final QuestionReviewService service;

    @PutMapping("/{questionId}/review")
    public QuestionReviewResponse review(
            @PathVariable @NotBlank String questionId,
            @Valid @RequestBody QuestionReviewRequest request) {
        return service.review(questionId, request);
    }
}
