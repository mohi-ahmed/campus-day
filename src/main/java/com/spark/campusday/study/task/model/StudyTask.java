package com.spark.campusday.study.task.model;

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
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_tasks")
@CompoundIndexes({
        @CompoundIndex(name = "study_task_student_due", def = "{'studentId': 1, 'dueDate': 1, 'dueTime': 1}"),
        @CompoundIndex(name = "study_task_student_status", def = "{'studentId': 1, 'status': 1, 'dueDate': 1}")
})
public class StudyTask {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotBlank
    @Size(max = 200)
    private String title;

    /** User-defined value. Subjects are intentionally not hardcoded. */
    @NotBlank
    @Size(max = 120)
    private String subject;

    /** User-defined value such as Study, Assignment, or Coding exercise. */
    @NotBlank
    @Size(max = 80)
    private String taskType;

    @NotNull
    private TaskPriority priority;

    @NotNull
    private LocalDate dueDate;

    private LocalTime dueTime;
    private Integer estimatedMinutes;

    @NotNull
    private TaskRepeat repeat;

    @Size(max = 1000)
    private String note;

    @NotNull
    private TaskStatus status;

    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
