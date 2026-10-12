package com.spark.campusday.careerprep.question.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record QuestionSource(
        @NotBlank @Size(max = 200) String name,
        @NotNull QuestionSourceType type,
        @Size(max = 2000) String url,
        @Size(max = 300) String externalId,
        @Size(max = 200) String licenseName,
        @Size(max = 1000) String attribution,
        Instant retrievedAt) {
}
