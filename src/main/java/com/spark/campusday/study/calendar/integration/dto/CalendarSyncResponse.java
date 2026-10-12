package com.spark.campusday.study.calendar.integration.dto;

import java.time.Instant;

public record CalendarSyncResponse(
        int created,
        int updated,
        int deleted,
        Instant syncedAt
) {
}
