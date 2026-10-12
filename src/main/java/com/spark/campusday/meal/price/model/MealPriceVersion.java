package com.spark.campusday.meal.price.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Append-only price revision. Latest effectiveAt wins for a menu target. */
@Getter
@Document(collection = "final_year_meal_prices")
@CompoundIndex(name = "price_target_versions", def = "{'menuId': 1, 'target': 1, 'itemId': 1, 'effectiveAt': -1}", unique = true)
public class MealPriceVersion {
    @Id
    private String id;
    private String menuId;
    private LocalDate mealDate;
    private PriceTarget target;
    private String itemId;
    private BigDecimal amount;
    private String setByStudentId;
    private Instant effectiveAt;

    @CreatedDate
    private Instant createdAt;

    protected MealPriceVersion() {
        // Spring Data MongoDB
    }

    public MealPriceVersion(String menuId, LocalDate mealDate, PriceTarget target, String itemId,
                            BigDecimal amount, String setByStudentId, Instant effectiveAt) {
        this.menuId = menuId;
        this.mealDate = mealDate;
        this.target = target;
        this.itemId = itemId;
        this.amount = amount;
        this.setByStudentId = setByStudentId;
        this.effectiveAt = effectiveAt;
    }
}
