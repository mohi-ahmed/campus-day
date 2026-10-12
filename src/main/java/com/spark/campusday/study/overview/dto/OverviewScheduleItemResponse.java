package com.spark.campusday.study.overview.dto;

import java.time.LocalTime;

public record OverviewScheduleItemResponse(
        String id,
        OverviewScheduleItemType type,
        String title,
        String subject,
        String itemType,
        LocalTime startTime,
        LocalTime endTime,
        boolean completed
) {
}
