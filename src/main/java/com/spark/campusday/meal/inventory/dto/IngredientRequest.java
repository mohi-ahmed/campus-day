package com.spark.campusday.meal.inventory.dto;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredientRequest(@NotBlank @Size(max = 100) String name,
                                @NotNull IngredientUnit unit) {}
