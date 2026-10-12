package com.spark.campusday.meal.order.dto;

import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;

public record OrderResponse(String id, String studentId, LocalDate mealDate, String menuId,
                            boolean breakfast, FinalYearMealOrder.MealChoice lunch,
                            FinalYearMealOrder.MealChoice dinner, OrderStatus status,
                            boolean canEdit, boolean canCancel, Instant editCutoffAt,
                            Instant cancelCutoffAt, Instant createdAt, Instant updatedAt) {}
