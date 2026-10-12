package com.spark.campusday.study.insights.dto;

public record SyllabusInsightsResponse(
        int totalTopics,
        int completedTopics,
        int revisionTopics,
        int learningTopics,
        int notStartedTopics,
        int overdueTopics,
        double completionPercentage
) {
}
