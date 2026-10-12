package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.QuestionImportStatus;

import java.time.Instant;
import java.util.List;

public record QuestionImportResponse(
        String importBatchId,
        String batchKey,
        QuestionImportStatus status,
        int receivedCount,
        int insertedCount,
        int skippedCount,
        int failedCount,
        List<String> errors,
        Instant createdAt,
        Instant updatedAt) {
}
