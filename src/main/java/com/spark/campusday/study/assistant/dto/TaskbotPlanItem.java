package com.spark.campusday.study.assistant.dto;

import com.spark.campusday.study.assistant.model.TaskbotPlanItemType;
import com.spark.campusday.study.assistant.serialization.FlexibleLocalTimeDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskbotPlanItem(
        TaskbotPlanItemType type,
        String title,
        String subject,
        LocalDate date,
        @JsonDeserialize(using = FlexibleLocalTimeDeserializer.class)
        LocalTime startTime,
        Integer durationMinutes,
        String sourceTaskId,
        String reason
) {
}
