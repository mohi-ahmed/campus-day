package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.BcsExamType;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.model.QuestionKind;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
import com.spark.campusday.careerprep.question.model.QuestionOption;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record QuestionImportItem(
        @Size(max = 300) String externalId,
        BcsExamType examType,
        @Min(1) @Max(999) Integer examNumber,
        @Min(1900) @Max(2200) Integer examLabelYear,
        LocalDate preliminaryHeldDate,
        @Size(max = 40) String examSetCode,
        @Min(1) @Max(1000) Integer questionNumber,
        @NotNull QuestionLanguage language,
        @NotBlank @Size(max = 80) String subjectCode,
        @NotBlank @Size(max = 100) String topicCode,
        @Size(max = 120) String subTopicCode,
        @Size(max = 20) List<@Size(max = 180) String> conceptCodes,
        @NotNull QuestionKind questionKind,
        @NotBlank @Size(max = 10000) String questionText,
        @Valid @NotEmpty @Size(min = 2, max = 8) List<QuestionOption> options,
        @NotBlank @Size(max = 8) String correctOptionKey,
        @Size(max = 20000) String explanation,
        QuestionDifficulty difficulty,
        @Size(max = 10) List<@Size(max = 2000) String> questionImageUrls,
        @Size(max = 10) List<@Size(max = 2000) String> explanationImageUrls,
        @Size(max = 30) List<@Size(max = 100) String> tags,
        boolean timeSensitive,
        LocalDate validFrom,
        LocalDate validUntil,
        @DecimalMin("0.0") @DecimalMax("1.0") Double answerConfidence,
        @DecimalMin("0.0") @DecimalMax("1.0") Double labelConfidence,
        Map<String, Object> rawPayload) {
}
