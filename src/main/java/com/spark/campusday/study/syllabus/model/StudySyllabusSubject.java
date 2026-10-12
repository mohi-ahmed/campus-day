package com.spark.campusday.study.syllabus.model;

import jakarta.validation.constraints.NotBlank;
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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_syllabus_subjects")
@CompoundIndexes({
        @CompoundIndex(name = "syllabus_subject_student_name_unique",
                def = "{'studentId': 1, 'nameKey': 1}", unique = true),
        @CompoundIndex(name = "syllabus_subject_student_active_order",
                def = "{'studentId': 1, 'active': 1, 'displayOrder': 1}")
})
public class StudySyllabusSubject {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 120)
    private String nameKey;

    @Size(max = 500)
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;

    private int displayOrder;
    private boolean active;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
