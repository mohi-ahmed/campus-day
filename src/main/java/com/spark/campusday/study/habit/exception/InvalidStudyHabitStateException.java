package com.spark.campusday.study.habit.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidStudyHabitStateException extends InvalidBusinessStateException {
    public InvalidStudyHabitStateException(String message) {
        super(message);
    }
}
