package com.spark.campusday.study.overview.dto;

import com.spark.campusday.study.task.model.TaskPriority;
import com.spark.campusday.study.task.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record OverviewTaskResponse(
        String id,
        String title,
        String subject,
        String taskType,
        TaskPriority priority,
        LocalDate dueDate,
        LocalTime dueTime,
        Integer estimatedMinutes,
        TaskStatus status
) {
}
