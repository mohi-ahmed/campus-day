package com.spark.campusday.meal.price.dto;

import com.spark.campusday.meal.price.model.PriceTarget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PriceVersionResponse(String id, String menuId, LocalDate mealDate, PriceTarget target,
                                   String itemId, BigDecimal amount, String setByStudentId,
                                   Instant effectiveAt) {}
