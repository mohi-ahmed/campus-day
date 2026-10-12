package com.spark.campusday.careerprep.question.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bcs_questions")
@CompoundIndexes({
        @CompoundIndex(name = "question_public_subject",
                def = "{'status': 1, 'subjectCode': 1, '_id': 1}"),
        @CompoundIndex(name = "question_public_topic",
                def = "{'status': 1, 'subjectCode': 1, 'topicCode': 1, '_id': 1}"),
        @CompoundIndex(name = "question_public_exam",
                def = "{'status': 1, 'examType': 1, 'examNumber': 1, '_id': 1}"),
        @CompoundIndex(name = "question_exam_sequence",
                def = "{'examType': 1, 'examNumber': 1, 'examSetCode': 1, 'questionNumber': 1}"),
        @CompoundIndex(name = "question_concept_filter",
                def = "{'status': 1, 'conceptCodes': 1, '_id': 1}"),
        @CompoundIndex(name = "question_training_export",
                def = "{'trainingEligible': 1, '_id': 1}"),
        @CompoundIndex(name = "question_prediction_cursor",
                def = "{'predictionEligible': 1, '_id': 1}"),
        @CompoundIndex(name = "question_import_batch",
                def = "{'importBatchId': 1, '_id': 1}")
})
public class BcsQuestion {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "question_id_unique", unique = true)
    @Size(max = 160)
    private String questionId;

    @NotBlank
    @Indexed(name = "question_source_key_unique", unique = true)
    @Size(max = 64)
    private String sourceKey;

    @NotBlank
    @Indexed(name = "question_fingerprint")
    @Size(min = 64, max = 64)
    private String fingerprint;

    private BcsExamType examType;

    @Min(1)
    @Max(999)
    private Integer examNumber;

    @Min(1900)
    @Max(2200)
    private Integer examLabelYear;

    private LocalDate preliminaryHeldDate;

    @Size(max = 40)
    private String examSetCode;

    @Min(1)
    @Max(1000)
    private Integer questionNumber;

    @NotNull
    private QuestionLanguage language;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9_]{2,80}")
    private String subjectCode;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9_]{2,100}")
    private String topicCode;

    @Pattern(regexp = "[A-Z0-9_]{2,120}")
    private String subTopicCode;

    @Size(max = 20)
    private List<@Pattern(regexp = "[A-Z0-9_.]{2,180}") String> conceptCodes;

    @NotNull
    private QuestionKind questionKind;

    @NotBlank
    @Size(max = 10000)
    private String questionText;

    @NotBlank
    @Size(max = 10000)
    private String normalizedQuestionText;

    @Valid
    @NotEmpty
    @Size(min = 2, max = 8)
    private List<QuestionOption> options;

    @NotBlank
    @Size(max = 8)
    private String correctOptionKey;

    @Size(max = 20000)
    private String explanation;

    @NotNull
    private QuestionDifficulty difficulty;

    @Size(max = 10)
    private List<@Size(max = 2000) String> questionImageUrls;

    @Size(max = 10)
    private List<@Size(max = 2000) String> explanationImageUrls;

    @Size(max = 30)
    private List<@Pattern(regexp = "[A-Z0-9_.-]{2,100}") String> tags;

    private boolean timeSensitive;
    private LocalDate validFrom;
    private LocalDate validUntil;

    @Valid
    @NotNull
    private QuestionSource source;

    @NotNull
    private ContentRightsStatus rightsStatus;

    @NotNull
    private QuestionStatus status;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double answerConfidence;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double labelConfidence;

    private boolean trainingEligible;
    private boolean predictionEligible;

    @NotBlank
    @Size(max = 80)
    private String importBatchId;

    @Min(1)
    private int schemaVersion;

    @Size(max = 120)
    private String reviewedBy;

    private Instant reviewedAt;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
