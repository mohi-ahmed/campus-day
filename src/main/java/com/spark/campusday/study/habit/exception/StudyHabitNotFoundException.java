package com.spark.campusday.study.habit.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class StudyHabitNotFoundException extends ResourceNotFoundException {
    public StudyHabitNotFoundException(String habitId) {
        super("Study habit not found: " + habitId);
    }
}
