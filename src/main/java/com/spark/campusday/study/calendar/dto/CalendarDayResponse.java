package com.spark.campusday.study.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarDayResponse(
        LocalDate date,
        CalendarDaySummaryResponse summary,
        List<CalendarEventResponse> events
) {
}
