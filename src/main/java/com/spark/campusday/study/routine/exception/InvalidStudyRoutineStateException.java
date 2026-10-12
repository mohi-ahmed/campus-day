package com.spark.campusday.study.routine.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidStudyRoutineStateException extends InvalidBusinessStateException {
    public InvalidStudyRoutineStateException(String message) {
        super(message);
    }
}
