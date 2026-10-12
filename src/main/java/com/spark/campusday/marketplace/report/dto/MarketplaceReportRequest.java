package com.spark.campusday.marketplace.report.dto;

import com.spark.campusday.marketplace.report.model.MarketplaceReportReason;
import com.spark.campusday.marketplace.report.model.MarketplaceReportTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarketplaceReportRequest(
        @NotBlank @Size(max = 64) String reporterStudentId,
        @NotNull MarketplaceReportTarget targetType,
        @NotBlank @Size(max = 64) String targetId,
        @NotNull MarketplaceReportReason reason,
        @Size(max = 2000) String details
) {
}
