package com.spark.campusday.meal.cookingplan.dto;

import com.spark.campusday.meal.model.MealType;

import java.time.LocalDate;
import java.util.List;

public record CookingPlanResponse(LocalDate mealDate, MealDemand breakfast, MealDemand lunch,
                                  MealDemand dinner) {
    public record MealDemand(MealType mealType, boolean available, String menuDescription,
                             String fullMealDescription, int yearsOneToFourBaseline,
                             int reportedAbsent, int yearsOneToFourPlanned,
                             int finalYearPackagesOrFullMeals, int totalPackagesOrFullMeals,
                             List<ItemDemand> finalYearItems) {}

    public record ItemDemand(String itemId, String name, String menuQuantity,
                             int orderedQuantity) {}
}
