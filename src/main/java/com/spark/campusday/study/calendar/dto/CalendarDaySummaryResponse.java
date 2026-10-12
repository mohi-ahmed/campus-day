package com.spark.campusday.study.calendar.dto;

import java.time.LocalDate;

public record CalendarDaySummaryResponse(
        LocalDate date,
        int totalEvents,
        int completedEvents,
        int pendingEvents,
        int overdueTasks
) {
}
