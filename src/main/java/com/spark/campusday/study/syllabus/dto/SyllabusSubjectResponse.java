package com.spark.campusday.study.syllabus.dto;

import java.time.Instant;

public record SyllabusSubjectResponse(
        String id,
        String name,
        String description,
        String color,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
