package com.spark.campusday.study.habit.model;

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
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_habits")
@CompoundIndexes({
        @CompoundIndex(name = "habit_student_name_unique",
                def = "{'studentId': 1, 'nameKey': 1}", unique = true),
        @CompoundIndex(name = "habit_student_active_order",
                def = "{'studentId': 1, 'active': 1, 'displayOrder': 1}")
})
public class StudyHabit {
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

    @NotEmpty
    private Set<DayOfWeek> repeatDays;

    private int displayOrder;
    private boolean active;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
