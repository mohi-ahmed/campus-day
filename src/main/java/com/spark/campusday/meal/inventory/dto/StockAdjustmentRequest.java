package com.spark.campusday.meal.inventory.dto;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockAdjustmentRequest(@NotNull String ingredientId, @NotNull IngredientUnit unit,
                                     @NotNull StockMovementType type,
                                     @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity,
                                     @NotBlank @Size(max = 500) String note) {}
