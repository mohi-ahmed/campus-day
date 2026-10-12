package com.spark.campusday.study.calendar.integration.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CalendarSyncRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {
}
