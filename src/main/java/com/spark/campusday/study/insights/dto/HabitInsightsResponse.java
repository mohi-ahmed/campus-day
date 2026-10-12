package com.spark.campusday.study.insights.dto;

import java.util.List;

public record HabitInsightsResponse(
        int activeHabits,
        int scheduledCompletions,
        int completedCompletions,
        double completionPercentage,
        List<HabitPerformanceResponse> habits
) {
}
