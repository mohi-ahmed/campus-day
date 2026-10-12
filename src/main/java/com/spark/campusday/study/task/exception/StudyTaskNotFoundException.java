package com.spark.campusday.study.task.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class StudyTaskNotFoundException extends ResourceNotFoundException {
    public StudyTaskNotFoundException(String taskId) {
        super("Study task not found: " + taskId);
    }
}
