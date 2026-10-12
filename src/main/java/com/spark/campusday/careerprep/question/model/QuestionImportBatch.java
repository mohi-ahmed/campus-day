package com.spark.campusday.careerprep.question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bcs_question_import_batches")
@CompoundIndex(name = "question_import_source_created",
        def = "{'sourceName': 1, 'createdAt': -1}")
public class QuestionImportBatch {
    @Id
    private String id;

    @Indexed(name = "question_import_batch_key_unique", unique = true)
    private String batchKey;

    private String sourceName;
    private QuestionSourceType sourceType;
    private String sourceUrl;
    private ContentRightsStatus rightsStatus;
    private int schemaVersion;
    private QuestionImportStatus status;
    private int receivedCount;
    private int insertedCount;
    private int skippedCount;
    private int failedCount;
    private List<String> errors;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
