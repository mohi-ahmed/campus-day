package com.spark.campusday.study.syllabus.dto;

import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import jakarta.validation.constraints.NotNull;

public record SyllabusTopicStatusRequest(
        @NotNull SyllabusTopicStatus status
) {
}
