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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bcs_taxonomy_nodes")
@CompoundIndex(name = "taxonomy_parent_level_active",
        def = "{'parentCode': 1, 'level': 1, 'active': 1, 'displayOrder': 1}")
public class BcsTaxonomyNode {
    @Id
    private String id;

    @Indexed(name = "taxonomy_code_unique", unique = true)
    private String code;

    private String parentCode;
    private TaxonomyLevel level;
    private String nameEn;
    private String nameBn;
    private Double syllabusMarks;
    private String syllabusVersion;
    private int displayOrder;
    private boolean active;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
