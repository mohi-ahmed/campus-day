package com.spark.campusday.study.calendar.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarEventResponse(
        String id,
        CalendarEventType type,
        String title,
        String subject,
        String itemType,
        String description,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean allDay,
        boolean completed,
        String status,
        String priority
) {
}
