package com.spark.campusday.meal.price.dto;

import com.spark.campusday.meal.price.model.PriceTarget;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SetPriceRequest(@NotNull LocalDate mealDate, @NotNull PriceTarget target,
                              String itemId, @NotNull @DecimalMin("0.00")
                              @Digits(integer = 10, fraction = 2) BigDecimal amount) {}
