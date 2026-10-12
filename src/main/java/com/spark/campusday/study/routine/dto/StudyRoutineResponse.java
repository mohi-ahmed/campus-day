package com.spark.campusday.study.routine.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

public record StudyRoutineResponse(
        String id,
        String subject,
        String blockType,
        String topic,
        String location,
        Set<DayOfWeek> days,
        LocalTime startTime,
        LocalTime endTime,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
