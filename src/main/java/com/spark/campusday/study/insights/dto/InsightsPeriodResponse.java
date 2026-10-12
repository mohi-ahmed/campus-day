package com.spark.campusday.study.insights.dto;

import java.time.LocalDate;

public record InsightsPeriodResponse(
        LocalDate from,
        LocalDate to,
        int totalDays
) {
}
