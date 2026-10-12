package com.spark.campusday.careerprep.question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
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
@Document(collection = "bcs_question_classifications")
@CompoundIndex(name = "classification_review_confidence",
        def = "{'reviewStatus': 1, 'confidence': -1, '_id': 1}")
public class QuestionClassificationProposal {
    @Id
    private String id;

    @Indexed(name = "classification_question_unique", unique = true)
    private String questionId;

    private String subjectCode;
    private String taxonomyVersion;
    private String proposedTopicCode;
    private String proposedSubTopicCode;
    private List<String> proposedConceptCodes;
    private String method;
    private double confidence;
    private List<String> matchedSignals;
    private ClassificationReviewStatus reviewStatus;
    private String reviewedBy;
    private Instant reviewedAt;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
