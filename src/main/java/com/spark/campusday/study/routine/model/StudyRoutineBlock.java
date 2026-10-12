package com.spark.campusday.study.routine.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_routine_blocks")
@CompoundIndexes({
        @CompoundIndex(name = "routine_student_active", def = "{'studentId': 1, 'active': 1}"),
        @CompoundIndex(name = "routine_student_days_start", def = "{'studentId': 1, 'days': 1, 'startTime': 1}")
})
public class StudyRoutineBlock {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    /** User-defined value. Subjects are intentionally not hardcoded. */
    @NotBlank
    @Size(max = 120)
    private String subject;

    /** User-defined value such as Class, Self-study, or Lab class. */
    @NotBlank
    @Size(max = 80)
    private String blockType;

    @NotBlank
    @Size(max = 200)
    private String topic;

    @Size(max = 200)
    private String location;

    @NotEmpty
    private Set<DayOfWeek> days;

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
