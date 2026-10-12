package com.spark.campusday.study.overview.dto;

public record OverviewFocusSummaryResponse(
        int todayMinutes,
        int weeklyMinutes,
        int weeklySessions
) {
}
