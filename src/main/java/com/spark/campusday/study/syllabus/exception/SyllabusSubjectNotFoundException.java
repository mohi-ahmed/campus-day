package com.spark.campusday.study.syllabus.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class SyllabusSubjectNotFoundException extends ResourceNotFoundException {
    public SyllabusSubjectNotFoundException(String subjectId) {
        super("Syllabus subject not found: " + subjectId);
    }
}
