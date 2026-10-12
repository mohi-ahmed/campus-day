package com.spark.campusday.study.focus.exception;

import com.spark.campusday.common.exception.ResourceNotFoundException;

public class FocusSessionNotFoundException extends ResourceNotFoundException {
    public FocusSessionNotFoundException(String sessionId) {
        super("Focus session not found: " + sessionId);
    }
}
