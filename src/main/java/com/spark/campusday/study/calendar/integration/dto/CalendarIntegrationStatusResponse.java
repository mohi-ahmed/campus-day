package com.spark.campusday.study.calendar.integration.dto;

import java.time.Instant;

public record CalendarIntegrationStatusResponse(
        boolean googleConfigured,
        boolean googleConnected,
        Instant googleConnectedAt,
        Instant googleLastSyncedAt,
        boolean appleExportAvailable
) {
}
