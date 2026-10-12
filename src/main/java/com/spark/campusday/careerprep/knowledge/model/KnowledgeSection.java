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
@Document(collection = "bcs_knowledge_sections")
@CompoundIndexes({
        @CompoundIndex(name = "knowledge_section_source_ordinal_unique",
                def = "{'sourceId': 1, 'ordinal': 1}", unique = true),
        @CompoundIndex(name = "knowledge_section_source_checksum_unique",
                def = "{'sourceId': 1, 'sha256': 1}", unique = true),
        @CompoundIndex(name = "knowledge_section_source_pages",
                def = "{'sourceId': 1, 'pageStart': 1, 'pageEnd': 1}"),
        @CompoundIndex(name = "knowledge_section_topic",
                def = "{'topicCode': 1, 'approved': 1, '_id': 1}"),
        @CompoundIndex(name = "knowledge_section_concepts",
                def = "{'conceptCodes': 1, 'approved': 1, '_id': 1}")
})
public class KnowledgeSection {
    @Id
    private String id;

    @Indexed(name = "knowledge_section_id_unique", unique = true)
    private String sectionId;

    private String sourceId;
    private int ordinal;
    private String chapter;
    private String heading;
    private Integer pageStart;
    private Integer pageEnd;
    private QuestionLanguage language;
    private String normalizedText;
    private String sha256;
    private String subjectCode;
    private String topicCode;
    private String subTopicCode;
    private List<String> conceptCodes;
    private ContentRightsStatus rightsStatus;
    private boolean approved;
    private boolean verificationEligible;
    private boolean trainingEligible;
    private String extractionMethod;
    private String extractionVersion;
    private Double extractionQuality;
    private boolean requiresOcr;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
