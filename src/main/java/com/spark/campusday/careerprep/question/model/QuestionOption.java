package com.spark.campusday.careerprep.question.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionOption(
        @NotBlank @Size(max = 8) String key,
        @NotBlank @Size(max = 2000) String text) {
}
