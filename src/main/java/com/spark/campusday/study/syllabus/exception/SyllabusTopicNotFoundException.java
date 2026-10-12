package com.spark.campusday.study.syllabus.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class SyllabusTopicNotFoundException extends ResourceNotFoundException {
    public SyllabusTopicNotFoundException(String topicId) {
        super("Syllabus topic not found: " + topicId);
    }
}
