package com.spark.campusday.meal.managementperiod.dto;

import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ManagementPeriodResponse(
        String id,
        int year,
        int month,
        LocalDate startDate,
        LocalDate endDate,
        ManagementPeriodStatus status,
        List<ManagerSummaryResponse> managers,
        Instant createdAt,
        Instant updatedAt
) {
}
