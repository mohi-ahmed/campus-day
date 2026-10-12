package com.spark.campusday.study.focus.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidFocusSessionStateException extends InvalidBusinessStateException {
    public InvalidFocusSessionStateException(String message) {
        super(message);
    }
}
