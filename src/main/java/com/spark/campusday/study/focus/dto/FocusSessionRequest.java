package com.spark.campusday.study.focus.dto;

import com.spark.campusday.study.focus.model.FocusSessionOutcome;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public record FocusSessionRequest(
        @NotBlank @Size(max = 100) String taskId,
        @NotNull LocalDate focusDate,
        @NotNull Instant startedAt,
        @NotNull Instant endedAt,
        @Min(5) @Max(180) int plannedMinutes,
        @Min(1) @Max(180) int focusedMinutes,
        @NotNull FocusSessionOutcome outcome
) {
}
