package com.spark.campusday.study.syllabus.dto;

import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;

import java.time.Instant;
import java.time.LocalDate;

public record SyllabusTopicResponse(
        String id,
        String syllabusSubjectId,
        String title,
        String note,
        SyllabusTopicStatus status,
        int displayOrder,
        LocalDate targetDate,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
