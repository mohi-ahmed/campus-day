package com.spark.campusday.study.habit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.Set;

public record StudyHabitRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotEmpty Set<@NotNull DayOfWeek> repeatDays,
        @Min(0) int displayOrder
) {
}
