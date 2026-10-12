package com.spark.campusday.autopsy.report.dto;

import com.spark.campusday.autopsy.report.model.AutopsyReportStatus;

import java.time.Instant;

public record ReportResponse(
        String id,
        String postId,
        AutopsyReportStatus status,
        Instant createdAt
) {
}
