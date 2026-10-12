package com.spark.campusday.study.insights.dto;

public record HabitPerformanceResponse(
        String habitId,
        String name,
        int scheduledDays,
        int completedDays,
        double completionPercentage,
        int currentStreakDays,
        int longestStreakDays
) {
}
