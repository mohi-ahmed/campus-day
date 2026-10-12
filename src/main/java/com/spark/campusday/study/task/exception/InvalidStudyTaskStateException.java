package com.spark.campusday.study.task.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidStudyTaskStateException extends InvalidBusinessStateException {
    public InvalidStudyTaskStateException(String message) {
        super(message);
    }
}
