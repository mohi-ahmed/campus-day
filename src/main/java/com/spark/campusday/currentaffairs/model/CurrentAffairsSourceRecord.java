package com.spark.campusday.currentaffairs.model;

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
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "current_affairs_source_records")
@CompoundIndexes({
        @CompoundIndex(name = "current_affairs_source_evidence_unique",
                def = "{'provider': 1, 'externalArticleId': 1, 'rawChecksum': 1}", unique = true),
        @CompoundIndex(name = "current_affairs_source_batch",
                def = "{'importBatchId': 1, 'ordinal': 1}")
})
public class CurrentAffairsSourceRecord {
    @Id
    private String id;
    private CurrentAffairsProvider provider;
    private String externalArticleId;
    private String importBatchId;
    private int ordinal;
    private String schemaVersion;
    private String rawChecksum;
    private Map<String, Object> payload;
    private CurrentAffairsRightsStatus rightsStatus;
    private CurrentAffairsVerificationStatus verificationStatus;

    @CreatedDate
    private Instant createdAt;
}
