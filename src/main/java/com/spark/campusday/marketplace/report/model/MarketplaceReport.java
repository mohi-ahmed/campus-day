package com.spark.campusday.marketplace.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_reports")
@CompoundIndexes({
        @CompoundIndex(name = "marketplace_report_status_created", def = "{'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_report_duplicate", def = "{'reporterStudentId': 1, 'targetType': 1, 'targetId': 1}", unique = true)
})
public class MarketplaceReport {
    @Id
    private String id;
    private String reporterStudentId;
    private MarketplaceReportTarget targetType;
    private String targetId;
    private MarketplaceReportReason reason;
    private String details;

    @Builder.Default
    private MarketplaceReportStatus status = MarketplaceReportStatus.OPEN;

    @CreatedDate
    private Instant createdAt;
}
