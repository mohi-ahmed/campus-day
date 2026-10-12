package com.spark.campusday.study.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarRangeResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<CalendarDayResponse> days
) {
}
