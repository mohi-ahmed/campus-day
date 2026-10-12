package com.spark.campusday.autopsy.report.dto;

import com.spark.campusday.autopsy.report.model.AutopsyReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank @Size(max = 128) String sessionId,
        @NotNull AutopsyReportReason reason,
        @Size(max = 500) String details
) {
}
