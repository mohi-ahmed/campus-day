package com.spark.campusday.study.preferences.dto;

import java.time.Instant;

public record StudyPreferencesResponse(
        String id,
        int focusMinutes,
        int shortBreakMinutes,
        int longBreakMinutes,
        int dailyFocusGoalMinutes,
        int weeklySessionGoal,
        int topicCompletionGoal,
        boolean taskReminders,
        boolean routineReminders,
        boolean examReminders,
        int reminderLeadMinutes,
        boolean morningDigest,
        boolean quietHoursEnabled,
        int dailyReminderLimit,
        Instant createdAt,
        Instant updatedAt
) {
}
