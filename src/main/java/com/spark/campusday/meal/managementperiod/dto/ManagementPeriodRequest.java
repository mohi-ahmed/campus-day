package com.spark.campusday.meal.managementperiod.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ManagementPeriodRequest(
        @NotNull @Min(1) Integer year,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Size(min = 2, max = 2) List<@NotBlank String> managerStudentIds
) {
}
