package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.TaxonomyLevel;

public record TaxonomyNodeResponse(
        String code,
        String parentCode,
        TaxonomyLevel level,
        String nameEn,
        String nameBn,
        Double syllabusMarks,
        String syllabusVersion,
        int displayOrder,
        boolean active) {
}
