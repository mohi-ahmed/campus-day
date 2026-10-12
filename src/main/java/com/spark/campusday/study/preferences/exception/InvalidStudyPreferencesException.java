package com.spark.campusday.study.preferences.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidStudyPreferencesException extends InvalidBusinessStateException {
    public InvalidStudyPreferencesException(String message) {
        super(message);
    }
}
