package com.spark.campusday.study.overview.dto;

public record OverviewTaskSummaryResponse(
        int total,
        int completed,
        int pending,
        int overdue,
        int scheduled
) {
}
