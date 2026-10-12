package com.spark.campusday.study.calendar.integration.model;

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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_calendar_connections")
@CompoundIndex(name = "calendar_connection_student_provider_unique",
        def = "{'studentId': 1, 'provider': 1}", unique = true)
public class StudyCalendarConnection {
    @Id
    private String id;

    /** Mongo student owner id, not the public registration id. */
    @NotBlank
    @Indexed
    private String studentId;

    @NotNull
    private CalendarIntegrationProvider provider;

    @NotBlank
    private String encryptedAccessToken;

    private String encryptedRefreshToken;

    @NotNull
    private Instant accessTokenExpiresAt;

    @NotNull
    private Instant connectedAt;

    /** Null keeps existing Mongo documents backward-compatible and means valid. */
    private Boolean authorizationValid;

    private Instant lastSyncedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
