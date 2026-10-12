package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.TaxonomyLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaxonomyNodeRequest(
        @NotBlank @Size(max = 180) String code,
        @Size(max = 180) String parentCode,
        @NotNull TaxonomyLevel level,
        @NotBlank @Size(max = 200) String nameEn,
        @Size(max = 200) String nameBn,
        @DecimalMin("0.0") Double syllabusMarks,
        @NotBlank @Size(max = 80) String syllabusVersion,
        int displayOrder,
        boolean active) {
}
