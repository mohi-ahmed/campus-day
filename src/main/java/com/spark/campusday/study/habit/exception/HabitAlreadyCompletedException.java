package com.spark.campusday.study.habit.exception;

import com.spark.campusday.common.exception.ConflictException;

import java.time.LocalDate;

public class HabitAlreadyCompletedException extends ConflictException {
    public HabitAlreadyCompletedException(String habitId, LocalDate completedOn) {
        super("Study habit " + habitId + " is already completed on " + completedOn);
    }

    public HabitAlreadyCompletedException(String habitId, LocalDate completedOn, Throwable cause) {
        super("Study habit " + habitId + " is already completed on " + completedOn, cause);
    }
}
