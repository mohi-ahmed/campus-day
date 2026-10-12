package com.spark.campusday.study.syllabus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SyllabusTopicRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 1000) String note,
        LocalDate targetDate,
        @Min(0) int displayOrder
) {
}
