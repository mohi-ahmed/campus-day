package com.spark.campusday.study.habit.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HabitCompletionRequest(
        @NotNull LocalDate completedOn
) {
}
