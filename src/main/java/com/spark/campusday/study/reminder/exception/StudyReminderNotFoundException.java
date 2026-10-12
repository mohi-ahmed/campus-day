package com.spark.campusday.study.reminder.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class StudyReminderNotFoundException extends ResourceNotFoundException {
    public StudyReminderNotFoundException(String reminderId) {
        super("Study reminder not found: " + reminderId);
    }
}
