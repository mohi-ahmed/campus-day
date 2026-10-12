package com.spark.campusday.study.habit.exception;

import com.spark.campusday.common.exception.ConflictException;

public class DuplicateStudyHabitException extends ConflictException {
    public DuplicateStudyHabitException(String habitName) {
        super("A study habit already exists with the name: " + habitName);
    }

    public DuplicateStudyHabitException(String habitName, Throwable cause) {
        super("A study habit already exists with the name: " + habitName, cause);
    }
}
