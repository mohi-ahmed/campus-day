package com.spark.campusday.study.habit.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record StudyHabitProgressResponse(
        String id,
        String name,
        String description,
        Set<DayOfWeek> repeatDays,
        int displayOrder,
        boolean active,
        Set<LocalDate> completedDates,
        Instant createdAt,
        Instant updatedAt
) {
}
