package com.spark.campusday.study.habit.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Document(collection = "study_habit_completions")
@CompoundIndexes({
        @CompoundIndex(name = "habit_completion_unique",
                def = "{'habitId': 1, 'completedOn': 1}", unique = true),
        @CompoundIndex(name = "habit_completion_student_date",
                def = "{'studentId': 1, 'completedOn': -1}")
})
public class StudyHabitCompletion {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotBlank
    @Indexed
    private String habitId;

    @NotNull
    @Indexed
    private LocalDate completedOn;

    @CreatedDate
    private Instant createdAt;
}
