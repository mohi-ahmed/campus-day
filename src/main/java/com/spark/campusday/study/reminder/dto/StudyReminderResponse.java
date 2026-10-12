package com.spark.campusday.study.reminder.dto;

import com.spark.campusday.study.reminder.model.StudyReminderStatus;
import com.spark.campusday.study.reminder.model.StudyReminderType;

import java.time.Instant;

public record StudyReminderResponse(
        String id,
        StudyReminderType type,
        String sourceId,
        String title,
        String message,
        String actionPath,
        Instant scheduledAt,
        StudyReminderStatus status,
        Instant deliveredAt,
        Instant readAt,
        Instant dismissedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
