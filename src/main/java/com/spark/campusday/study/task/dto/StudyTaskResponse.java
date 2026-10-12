package com.spark.campusday.study.task.dto;

import com.spark.campusday.study.task.model.TaskPriority;
import com.spark.campusday.study.task.model.TaskRepeat;
import com.spark.campusday.study.task.model.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record StudyTaskResponse(
        String id,
        String title,
        String subject,
        String taskType,
        TaskPriority priority,
        LocalDate dueDate,
        LocalTime dueTime,
        Integer estimatedMinutes,
        TaskRepeat repeat,
        String note,
        TaskStatus status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
