package com.spark.campusday.study.habit.dto;

import java.time.Instant;
import java.time.LocalDate;

public record HabitCompletionResponse(
        String id,
        String habitId,
        LocalDate completedOn,
        Instant createdAt
) {
}
