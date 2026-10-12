package com.spark.campusday.meal.absence.dto;

import com.spark.campusday.meal.model.MealType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AbsenceNoticeRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate,
                                   @NotEmpty List<@NotNull MealType> meals,
                                   @Size(max = 1000) String note) {}
