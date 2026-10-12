package com.spark.campusday.meal.managerapplication.dto;

import java.time.LocalDate;

public record MealManagerApplicationOverview(
        boolean eligible,
        boolean currentAccess,
        String managementPeriodId,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        MealManagerApplicationResponse application
) {
}
