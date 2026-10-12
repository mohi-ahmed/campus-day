package com.spark.campusday.study.routine.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class StudyRoutineNotFoundException extends ResourceNotFoundException {
    public StudyRoutineNotFoundException(String blockId) {
        super("Study routine block not found: " + blockId);
    }
}
