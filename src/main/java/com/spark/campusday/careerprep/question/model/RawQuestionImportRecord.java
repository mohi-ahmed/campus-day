package com.spark.campusday.careerprep.question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bcs_question_raw_imports")
@CompoundIndexes({
        @CompoundIndex(name = "raw_question_batch_ordinal_unique",
                def = "{'importBatchId': 1, 'ordinal': 1}", unique = true),
        @CompoundIndex(name = "raw_question_batch_external",
                def = "{'importBatchId': 1, 'externalId': 1}")
})
public class RawQuestionImportRecord {
    @Id
    private String id;
    private String importBatchId;
    private int ordinal;
    private String externalId;
    private String rawChecksum;
    private Map<String, Object> payload;
    private String normalizedQuestionId;
    private String error;

    @CreatedDate
    private Instant createdAt;
}
