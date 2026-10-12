package com.spark.campusday.study.overview.dto;

public record OverviewGoalSummaryResponse(
        int dailyFocusMinutes,
        int weeklySessions,
        int topicCompletions
) {
}
