package com.spark.campusday.study.habit.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;

public record StudyHabitResponse(
        String id,
        String name,
        String description,
        Set<DayOfWeek> repeatDays,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
