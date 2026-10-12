package com.spark.campusday.marketplace.report.dto;

import com.spark.campusday.marketplace.report.model.MarketplaceReportReason;
import com.spark.campusday.marketplace.report.model.MarketplaceReportStatus;
import com.spark.campusday.marketplace.report.model.MarketplaceReportTarget;

import java.time.Instant;

public record MarketplaceReportResponse(
        String id,
        String reporterStudentId,
        MarketplaceReportTarget targetType,
        String targetId,
        MarketplaceReportReason reason,
        String details,
        MarketplaceReportStatus status,
        Instant createdAt
) {
}
