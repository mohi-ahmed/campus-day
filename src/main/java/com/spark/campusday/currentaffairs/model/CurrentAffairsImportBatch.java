package com.spark.campusday.currentaffairs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "current_affairs_import_batches")
public class CurrentAffairsImportBatch {
    @Id
    private String id;
    private CurrentAffairsProvider provider;
    private String schemaVersion;

    @Indexed(name = "current_affairs_batch_started")
    private Instant startedAt;

    private Instant completedAt;
    private CurrentAffairsImportStatus status;
    private int fetchedCount;
    private int persistedCount;
    private int rejectedCount;
    private String error;
}
