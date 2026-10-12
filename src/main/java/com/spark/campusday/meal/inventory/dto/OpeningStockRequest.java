package com.spark.campusday.meal.inventory.dto;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OpeningStockRequest(@NotNull String ingredientId, @NotNull IngredientUnit unit,
                                  @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity) {}
