package com.spark.campusday.study.focus.dto;

import com.spark.campusday.study.focus.model.FocusSessionOutcome;

import java.time.Instant;
import java.time.LocalDate;

public record FocusSessionResponse(
        String id,
        String taskId,
        String taskTitle,
        String subject,
        LocalDate focusDate,
        Instant startedAt,
        Instant endedAt,
        int plannedMinutes,
        int focusedMinutes,
        FocusSessionOutcome outcome,
        Instant createdAt
) {
}
