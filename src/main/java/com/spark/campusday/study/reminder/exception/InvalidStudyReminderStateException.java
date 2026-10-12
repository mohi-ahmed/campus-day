package com.spark.campusday.study.reminder.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidStudyReminderStateException extends InvalidBusinessStateException {
    public InvalidStudyReminderStateException(String message) {
        super(message);
    }
}
