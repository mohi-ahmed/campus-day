package com.spark.campusday.meal.menu.dto;

import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;

import java.time.Instant;
import java.time.LocalDate;

public record MenuResponse(String id, LocalDate mealDate, MenuStatus status,
                           DailyMealMenu.Breakfast breakfast, DailyMealMenu.MainMeal lunch,
                           DailyMealMenu.MainMeal dinner, Instant createdAt, Instant updatedAt) {}
