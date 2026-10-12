package com.spark.campusday.careerprep.knowledge.model;

import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
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
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bcs_knowledge_sources")
@CompoundIndexes({
        @CompoundIndex(name = "knowledge_source_status_authority",
                def = "{'ingestionStatus': 1, 'authority': 1, '_id': 1}"),
        @CompoundIndex(name = "knowledge_source_subject_status",
                def = "{'subjectCodes': 1, 'ingestionStatus': 1, '_id': 1}")
})
public class KnowledgeSource {
    @Id
    private String id;

    @Indexed(name = "knowledge_source_id_unique", unique = true)
    private String sourceId;

    private KnowledgeSourceType sourceType;
    private KnowledgeAuthority authority;
    private String title;
    private String publisher;
    private String edition;
    private String academicYear;
    private QuestionLanguage language;
    private String officialLandingPageUrl;
    private String downloadUrl;
    private String storageUri;
    private String mediaType;
    private String sha256;
    private ContentRightsStatus rightsStatus;
    private String rightsNote;
    private String provenanceNote;
    private List<String> subjectCodes;
    private String taxonomyVersion;
    private KnowledgeIngestionStatus ingestionStatus;
    private boolean verificationEligible;
    private boolean trainingEligible;
    private Integer retainedSectionCount;
    private Long retainedCharacterCount;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
