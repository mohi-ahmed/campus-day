package com.spark.campusday.meal.absence.dto;

import com.spark.campusday.meal.model.MealType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AbsenceNoticeResponse(String id, LocalDate startDate, LocalDate endDate,
                                    List<MealType> meals, String note,
                                    Instant createdAt, Instant updatedAt) {}
