package com.spark.campusday.study.focus.model;

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
@Document(collection = "study_focus_sessions")
@CompoundIndexes({
        @CompoundIndex(name = "focus_student_date", def = "{'studentId': 1, 'focusDate': -1, 'startedAt': -1}"),
        @CompoundIndex(name = "focus_student_task", def = "{'studentId': 1, 'taskId': 1, 'startedAt': -1}")
})
public class FocusSession {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotBlank
    private String taskId;

    /** Snapshots keep history readable even after a task is edited or removed. */
    @NotBlank
    @Size(max = 200)
    private String taskTitle;

    @NotBlank
    @Size(max = 120)
    private String subject;

    @NotNull
    @Indexed
    private LocalDate focusDate;

    @NotNull
    private Instant startedAt;

    @NotNull
    private Instant endedAt;

    private int plannedMinutes;
    private int focusedMinutes;

    @NotNull
    private FocusSessionOutcome outcome;

    @CreatedDate
    private Instant createdAt;

}
