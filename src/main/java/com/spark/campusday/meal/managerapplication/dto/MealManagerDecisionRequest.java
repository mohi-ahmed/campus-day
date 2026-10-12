package com.spark.campusday.meal.managerapplication.dto;

import com.spark.campusday.meal.managerapplication.model.MealManagerDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MealManagerDecisionRequest(
        @NotBlank String token,
        @NotNull MealManagerDecision decision
) {
}
