package com.spark.campusday.study.insights.dto;

public record SubjectPerformanceResponse(
        String subject,
        String color,
        int focusedMinutes,
        int plannedMinutes,
        int completedTasks,
        int totalTasks,
        int completedTopics,
        int totalTopics,
        double taskCompletionPercentage,
        double syllabusCompletionPercentage
) {
}
