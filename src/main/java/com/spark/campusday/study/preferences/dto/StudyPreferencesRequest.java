package com.spark.campusday.study.preferences.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StudyPreferencesRequest(
        @Min(5) @Max(180) int focusMinutes,
        @Min(1) @Max(30) int shortBreakMinutes,
        @Min(5) @Max(60) int longBreakMinutes,
        @Min(1) @Max(1440) int dailyFocusGoalMinutes,
        @Min(1) @Max(100) int weeklySessionGoal,
        @Min(1) @Max(1000) int topicCompletionGoal,
        boolean taskReminders,
        boolean routineReminders,
        boolean examReminders,
        @Min(0) @Max(1440) int reminderLeadMinutes,
        boolean morningDigest,
        boolean quietHoursEnabled,
        @Min(1) @Max(20) int dailyReminderLimit
) {
}
