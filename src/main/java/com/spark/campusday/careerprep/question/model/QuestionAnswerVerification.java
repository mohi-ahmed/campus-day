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
@Document(collection = "bcs_question_answer_verifications")
@CompoundIndex(name = "answer_verification_status_question",
        def = "{'status': 1, 'questionId': 1}")
public class QuestionAnswerVerification {
    @Id
    private String id;

    @Indexed(name = "answer_verification_question_unique", unique = true)
    private String questionId;

    private String claimedOptionKey;
    private AnswerVerificationStatus status;
    private List<VerificationEvidence> evidence;
    private String verifiedOptionKey;
    private Double confidence;
    private String notes;
    private String reviewedBy;
    private Instant reviewedAt;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
