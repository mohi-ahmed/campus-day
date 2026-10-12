package com.spark.campusday.meal.inventory.dto;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import com.spark.campusday.meal.model.MealType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class InventoryResponses {
    private InventoryResponses() {}

    public record IngredientResponse(String id, String name, IngredientUnit unit,
                                     BigDecimal currentStock, boolean openingAllowed,
                                     Instant createdAt, Instant updatedAt) {}

    public record MovementResponse(String id, String ingredientId, long sequence,
                                   StockMovementType type, BigDecimal quantity,
                                   BigDecimal balanceAfter, IngredientUnit unit,
                                   LocalDate businessDate, Instant occurredAt,
                                   MealType mealType, String note, String managerStudentId,
                                   String sourceReference) {}

    public record MovementHistoryResponse(String ingredientId, List<MovementResponse> content, int page, int size,
                                          long totalElements, int totalPages) {}

    public record IssueLineResponse(String id, String ingredientId, String ingredientName,
                                    IngredientUnit unit, BigDecimal quantity, MealType mealType,
                                    LocalDate date, String note, Instant occurredAt) {}

    public record IssueHistoryResponse(LocalDate date, List<IssueLineResponse> content, int page, int size,
                                       long totalElements, int totalPages) {}
}
