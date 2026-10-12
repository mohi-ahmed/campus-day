package com.spark.campusday.careerprep.question.controller;

import com.spark.campusday.careerprep.question.dto.CursorPage;
import com.spark.campusday.careerprep.question.dto.TrainingQuestionResponse;
import com.spark.campusday.careerprep.question.service.QuestionQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Internal AI export API. It returns only rights-cleared and verified records. */
@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
@Validated
@RestController
@RequestMapping("/api/internal/career-prep/training/questions")
@RequiredArgsConstructor
public class QuestionTrainingController {
    private final QuestionQueryService service;

    @GetMapping
    public CursorPage<TrainingQuestionResponse> list(
            @RequestParam(defaultValue = "false") boolean predictionOnly,
            @RequestParam(required = false) String afterId,
            @RequestParam(defaultValue = "500") @Min(1) @Max(1000) int limit) {
        return service.findTrainingQuestions(predictionOnly, afterId, limit);
    }
}
