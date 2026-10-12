package com.spark.campusday.currentaffairs.dto;

public record CurrentAffairsImportResult(
        String batchId,
        int fetchedCount,
        int persistedCount,
        int rejectedCount
) {
}
