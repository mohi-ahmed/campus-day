package com.spark.campusday.study.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record StudyRoutineRequest(
        @NotBlank @Size(max = 120) String subject,
        @NotBlank @Size(max = 80) String blockType,
        @NotBlank @Size(max = 200) String topic,
        @Size(max = 200) String location,
        @NotEmpty Set<@NotNull DayOfWeek> days,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
