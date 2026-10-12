package com.spark.campusday.meal.managementperiod.dto;

import java.util.List;

public record ManagementPeriodHistoryResponse(
        List<ManagementPeriodResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages
) {
}
