package com.spark.campusday.study.syllabus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SyllabusSubjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
        @Min(0) int displayOrder
) {
}
