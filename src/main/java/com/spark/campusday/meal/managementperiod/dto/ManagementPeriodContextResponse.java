package com.spark.campusday.meal.managementperiod.dto;

public record ManagementPeriodContextResponse(
        ManagementPeriodResponse current,
        ManagementPeriodResponse previous,
        ManagementPeriodResponse next
) {
}
