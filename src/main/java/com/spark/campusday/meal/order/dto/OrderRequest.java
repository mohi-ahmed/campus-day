package com.spark.campusday.meal.order.dto;

import com.spark.campusday.meal.order.model.MealChoiceKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record OrderRequest(@NotNull LocalDate mealDate, boolean breakfast,
                           @NotNull @Valid MealChoiceRequest lunch,
                           @NotNull @Valid MealChoiceRequest dinner) {
    public record MealChoiceRequest(@NotNull MealChoiceKind kind, @Valid List<ItemQuantity> items) {}

    public record ItemQuantity(@NotBlank String itemId, @Positive int quantity) {}
}
