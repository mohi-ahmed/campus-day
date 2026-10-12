package com.spark.campusday.study.assistant.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidTaskbotResponseException extends InvalidBusinessStateException {
    public InvalidTaskbotResponseException(String message) {
        super(message);
    }
}
