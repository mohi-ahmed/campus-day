package com.spark.campusday.study.reminder.model;

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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_reminders")
@CompoundIndexes({
        @CompoundIndex(name = "reminder_student_status_schedule",
                def = "{'studentId': 1, 'status': 1, 'scheduledAt': 1}"),
        @CompoundIndex(name = "reminder_student_delivered_at",
                def = "{'studentId': 1, 'deliveredAt': 1}"),
        @CompoundIndex(name = "reminder_source_schedule_unique",
                def = "{'studentId': 1, 'type': 1, 'sourceId': 1, 'scheduledAt': 1}",
                unique = true)
})
public class StudyReminder {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String studentId;

    @NotNull
    private StudyReminderType type;

    /** Task, routine block, syllabus topic, or digest date identifier. */
    @NotBlank
    @Size(max = 100)
    private String sourceId;

    /** Snapshots keep delivered reminders readable after their source changes. */
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String message;

    /** Optional client route opened when the reminder is selected. */
    @Size(max = 500)
    private String actionPath;

    @NotNull
    @Indexed
    private Instant scheduledAt;

    @NotNull
    private StudyReminderStatus status;

    private Instant deliveredAt;
    private Instant readAt;
    private Instant dismissedAt;
    private Instant cancelledAt;
    private boolean userCancelled;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
