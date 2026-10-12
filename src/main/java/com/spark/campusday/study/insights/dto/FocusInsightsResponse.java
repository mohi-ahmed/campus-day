package com.spark.campusday.study.insights.dto;

public record FocusInsightsResponse(
        int totalMinutes,
        int totalSessions,
        int completedSessions,
        int stoppedSessions,
        double averageSessionMinutes,
        int activeDays,
        int currentStreakDays,
        int longestStreakDays
) {
}
