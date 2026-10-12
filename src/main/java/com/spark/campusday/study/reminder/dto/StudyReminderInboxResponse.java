package com.spark.campusday.study.reminder.dto;

import java.util.List;

public record StudyReminderInboxResponse(
        long unreadCount,
        long scheduledCount,
        List<StudyReminderResponse> reminders
) {
}
