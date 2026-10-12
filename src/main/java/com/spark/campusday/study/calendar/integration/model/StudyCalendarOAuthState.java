package com.spark.campusday.study.calendar.integration.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_calendar_oauth_states")
public class StudyCalendarOAuthState {
    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String state;

    @NotBlank
    @Indexed
    private String studentId;

    @NotNull
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
