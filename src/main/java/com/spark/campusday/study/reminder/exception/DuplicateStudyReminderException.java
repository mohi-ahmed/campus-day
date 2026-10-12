package com.spark.campusday.study.reminder.exception;

import com.spark.campusday.common.exception.ConflictException;

public class DuplicateStudyReminderException extends ConflictException {
    public DuplicateStudyReminderException(String sourceId) {
        super("A study reminder is already scheduled for source: " + sourceId);
    }

    public DuplicateStudyReminderException(String sourceId, Throwable cause) {
        super("A study reminder is already scheduled for source: " + sourceId, cause);
    }
}
