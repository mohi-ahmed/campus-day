package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionReviewRequest(
        @NotNull QuestionStatus status,
        @NotNull ContentRightsStatus rightsStatus,
        @DecimalMin("0.0") @DecimalMax("1.0") Double answerConfidence,
        @DecimalMin("0.0") @DecimalMax("1.0") Double labelConfidence,
        @NotBlank @Size(max = 120) String reviewerId) {
}
