package com.spark.campusday.study.assistant.dto;

import com.spark.campusday.study.task.model.TaskPriority;
import com.spark.campusday.study.task.model.TaskRepeat;
import com.spark.campusday.study.assistant.serialization.FlexibleLocalTimeDeserializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import tools.jackson.databind.annotation.JsonDeserialize;

public record TaskbotTaskDraft(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 120) String subject,
        @NotBlank @Size(max = 80) String taskType,
        @NotNull TaskPriority priority,
        @NotNull LocalDate dueDate,
        @JsonDeserialize(using = FlexibleLocalTimeDeserializer.class)
        LocalTime dueTime,
        @Min(1) @Max(1440) Integer estimatedMinutes,
        @NotNull TaskRepeat repeat,
        @Size(max = 1000) String note
) {
}
