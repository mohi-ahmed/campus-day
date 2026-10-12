package com.spark.campusday.meal.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MenuRequest(
        @NotNull LocalDate mealDate,
        @NotNull @Valid BreakfastRequest breakfast,
        @NotNull @Valid MainMealRequest lunch,
        @NotNull @Valid MainMealRequest dinner
) {
    public record BreakfastRequest(@NotBlank String packageDescription,
                                   boolean available, boolean orderingEnabled) {}

    public record MainMealRequest(@NotBlank String description, boolean available,
                                  boolean orderingEnabled, boolean fullMealAvailable,
                                  String fullMealDescription, @Valid List<ItemRequest> items) {}

    public record ItemRequest(String id, @NotBlank String name, String nameBn,
                              String quantity, boolean available) {}
}
