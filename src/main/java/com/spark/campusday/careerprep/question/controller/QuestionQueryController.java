package com.spark.campusday.careerprep.question.controller;

import com.spark.campusday.careerprep.question.dto.CursorPage;
import com.spark.campusday.careerprep.question.dto.QuestionDeliveryResponse;
import com.spark.campusday.careerprep.question.dto.TrainingQuestionResponse;
import com.spark.campusday.careerprep.question.model.BcsExamType;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.service.QuestionQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
@Validated
@RestController
@RequestMapping("/api/career-prep/questions")
@RequiredArgsConstructor
public class QuestionQueryController {
    private final QuestionQueryService service;

    @GetMapping
    public CursorPage<QuestionDeliveryResponse> list(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String topicCode,
            @RequestParam(required = false) BcsExamType examType,
            @RequestParam(required = false) Integer examNumber,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) String afterId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return service.findPublished(subjectCode, topicCode, examType, examNumber,
                difficulty, afterId, limit);
    }

    @GetMapping("/{questionId}")
    public QuestionDeliveryResponse get(@PathVariable @NotBlank String questionId) {
        return service.getPublished(questionId);
    }
}
