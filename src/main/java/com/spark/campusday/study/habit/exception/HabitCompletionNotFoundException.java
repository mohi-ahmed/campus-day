package com.spark.campusday.study.habit.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

import java.time.LocalDate;

public class HabitCompletionNotFoundException extends ResourceNotFoundException {
    public HabitCompletionNotFoundException(String habitId, LocalDate completedOn) {
        super("Study habit completion not found for " + habitId + " on " + completedOn);
    }
}
