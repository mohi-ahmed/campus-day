package com.spark.campusday.meal.price.dto;

import com.spark.campusday.meal.price.model.PriceTarget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplicablePriceResponse(String menuId, LocalDate mealDate, List<Entry> prices) {
    public record Entry(PriceTarget target, String itemId, String label,
                        BigDecimal amount, Instant effectiveAt) {
        public boolean pricePending() {
            return amount == null;
        }
    }
}
