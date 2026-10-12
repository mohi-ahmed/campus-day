package com.spark.campusday.meal.inventory.model;

import com.spark.campusday.meal.model.MealType;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Immutable ledger entry; its balanceAfter is the authoritative current stock when latest. */
@Getter
@Document(collection = "meal_stock_movements")
@CompoundIndexes({
        @CompoundIndex(name = "ingredient_sequence_unique", def = "{'ingredientId': 1, 'sequence': -1}", unique = true),
        @CompoundIndex(name = "kitchen_issue_date", def = "{'type': 1, 'businessDate': 1, 'occurredAt': -1}")
})
public class StockMovement {
    @Id
    private String id;
    private String ingredientId;
    private long sequence;
    private StockMovementType type;
    private BigDecimal quantity;
    private BigDecimal balanceAfter;
    private IngredientUnit unit;
    @Indexed private LocalDate businessDate;
    private Instant occurredAt;
    private MealType mealType;
    private String note;
    private String managerStudentId;
    @Indexed(unique = true, sparse = true)
    private String sourceReference;
    @CreatedDate
    private Instant createdAt;

    protected StockMovement() { // Spring Data MongoDB
    }

    public StockMovement(String ingredientId, long sequence, StockMovementType type, BigDecimal quantity,
                         BigDecimal balanceAfter, IngredientUnit unit, LocalDate businessDate,
                         Instant occurredAt, MealType mealType, String note, String managerStudentId) {
        this(ingredientId, sequence, type, quantity, balanceAfter, unit, businessDate,
                occurredAt, mealType, note, managerStudentId, null);
    }

    public StockMovement(String ingredientId, long sequence, StockMovementType type, BigDecimal quantity,
                         BigDecimal balanceAfter, IngredientUnit unit, LocalDate businessDate,
                         Instant occurredAt, MealType mealType, String note, String managerStudentId,
                         String sourceReference) {
        this.ingredientId = ingredientId;
        this.sequence = sequence;
        this.type = type;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.unit = unit;
        this.businessDate = businessDate;
        this.occurredAt = occurredAt;
        this.mealType = mealType;
        this.note = note;
        this.managerStudentId = managerStudentId;
        this.sourceReference = sourceReference;
    }
}
