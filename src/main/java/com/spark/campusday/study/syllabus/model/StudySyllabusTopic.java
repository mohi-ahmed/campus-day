package com.spark.campusday.study.syllabus.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_syllabus_topics")
@CompoundIndexes({
        @CompoundIndex(name = "syllabus_topic_subject_title_unique",
                def = "{'syllabusSubjectId': 1, 'titleKey': 1}", unique = true),
        @CompoundIndex(name = "syllabus_topic_subject_order",
                def = "{'syllabusSubjectId': 1, 'displayOrder': 1}"),
        @CompoundIndex(name = "syllabus_topic_student_status",
                def = "{'studentId': 1, 'status': 1}")
})
public class StudySyllabusTopic {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotBlank
    @Indexed
    private String syllabusSubjectId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 200)
    private String titleKey;

    @Size(max = 1000)
    private String note;

    @NotNull
    private SyllabusTopicStatus status;

    private int displayOrder;
    private LocalDate targetDate;
    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
