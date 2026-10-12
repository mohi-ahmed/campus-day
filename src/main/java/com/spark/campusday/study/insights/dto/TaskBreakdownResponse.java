package com.spark.campusday.study.insights.dto;

public record TaskBreakdownResponse(
        String label,
        int totalTasks,
        int completedTasks,
        int pendingTasks,
        double completionPercentage
) {
}
