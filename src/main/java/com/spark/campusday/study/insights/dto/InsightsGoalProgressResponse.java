package com.spark.campusday.study.insights.dto;

public record InsightsGoalProgressResponse(
        int dailyFocusGoalMinutes,
        int focusedMinutesToday,
        int weeklySessionGoal,
        int sessionsThisWeek,
        int topicCompletionGoal,
        int completedTopics
) {
}
