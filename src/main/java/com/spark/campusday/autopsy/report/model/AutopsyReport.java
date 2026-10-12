package com.spark.campusday.autopsy.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "autopsy_reports")
@CompoundIndex(name = "autopsy_report_post_actor", def = "{'postId': 1, 'actorHash': 1}", unique = true)
public class AutopsyReport {
    @Id
    private String id;

    private String postId;
    private String actorHash;
    private AutopsyReportReason reason;
    private String details;

    @Builder.Default
    private AutopsyReportStatus status = AutopsyReportStatus.PENDING;

    @CreatedDate
    private Instant createdAt;
}
