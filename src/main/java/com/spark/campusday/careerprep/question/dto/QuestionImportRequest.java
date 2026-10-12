package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record QuestionImportRequest(
        @NotBlank @Size(max = 120) String batchKey,
        @NotBlank @Size(max = 200) String sourceName,
        @NotNull QuestionSourceType sourceType,
        @Size(max = 2000) String sourceUrl,
        @NotNull ContentRightsStatus rightsStatus,
        @Size(max = 200) String licenseName,
        @Size(max = 1000) String attribution,
        Instant retrievedAt,
        @NotEmpty @Size(max = 10000)
        List<@Valid @NotNull QuestionImportItem> questions) {
}
