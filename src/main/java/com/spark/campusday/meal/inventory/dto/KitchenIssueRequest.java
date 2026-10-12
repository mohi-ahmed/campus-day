package com.spark.campusday.meal.inventory.dto;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.model.MealType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KitchenIssueRequest(@NotNull LocalDate date, @NotNull MealType mealType,
                                  @NotNull String ingredientId, @NotNull IngredientUnit unit,
                                  @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity,
                                  @Size(max = 500) String note) {}
