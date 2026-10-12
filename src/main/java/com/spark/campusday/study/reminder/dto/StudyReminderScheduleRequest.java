package com.spark.campusday.study.reminder.dto;

import com.spark.campusday.study.reminder.model.StudyReminderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Internal scheduling command; student identity comes from the authenticated boundary. */
public record StudyReminderScheduleRequest(
        @NotNull StudyReminderType type,
        @NotBlank @Size(max = 100) String sourceId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 500) String actionPath,
        @NotNull Instant scheduledAt
) {
}
