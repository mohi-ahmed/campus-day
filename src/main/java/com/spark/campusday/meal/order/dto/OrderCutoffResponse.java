package com.spark.campusday.meal.order.dto;

import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;

import java.time.Instant;
import java.time.LocalDate;

public record OrderCutoffResponse(LocalDate mealDate,
                                  OrderCutoffConfiguration.CutoffRule orderCutoff,
                                  OrderCutoffConfiguration.CutoffRule editCutoff,
                                  OrderCutoffConfiguration.CutoffRule cancelCutoff,
                                  Instant orderCutoffAt, Instant editCutoffAt,
                                  Instant cancelCutoffAt, Instant updatedAt) {}
