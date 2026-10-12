package com.spark.campusday.study.insights.dto;

import java.util.List;

public record TaskInsightsResponse(
        int totalTasks,
        int completedTasks,
        int pendingTasks,
        int overdueTasks,
        int scheduledTasks,
        double completionPercentage,
        List<TaskBreakdownResponse> byPriority,
        List<TaskBreakdownResponse> byType
) {
}
