package com.spark.campusday.meal.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record OrderCutoffRequest(@NotNull @Valid Rule orderCutoff,
                                 @NotNull @Valid Rule editCutoff,
                                 @NotNull @Valid Rule cancelCutoff) {
    public record Rule(@Min(0) int daysBeforeMeal, @NotNull LocalTime time) {}
}
