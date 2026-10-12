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
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_calendar_sync_items")
@CompoundIndex(name = "calendar_sync_connection_key_unique",
        def = "{'connectionId': 1, 'localEventKey': 1}", unique = true)
public class StudyCalendarSyncItem {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String connectionId;

    @NotBlank
    private String localEventKey;

    @NotNull
    @Indexed
    private LocalDate eventDate;

    @NotBlank
    private String externalEventId;

    private String eventFingerprint;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
