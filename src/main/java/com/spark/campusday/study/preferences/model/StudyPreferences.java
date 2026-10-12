package com.spark.campusday.study.preferences.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_preferences")
public class StudyPreferences {
    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String studentId;

    private int focusMinutes;
    private int shortBreakMinutes;
    private int longBreakMinutes;
    private int dailyFocusGoalMinutes;
    private int weeklySessionGoal;
    private int topicCompletionGoal;
    private boolean taskReminders;
    private boolean routineReminders;
    private boolean examReminders;
    private int reminderLeadMinutes;
    @NotNull
    private Boolean morningDigest;
    @NotNull
    private Boolean quietHoursEnabled;
    @NotNull
    private Integer dailyReminderLimit;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
