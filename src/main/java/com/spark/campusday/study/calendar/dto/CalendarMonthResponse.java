package com.spark.campusday.study.calendar.dto;

import java.util.List;

public record CalendarMonthResponse(
        int year,
        int month,
        List<CalendarDaySummaryResponse> days
) {
}
