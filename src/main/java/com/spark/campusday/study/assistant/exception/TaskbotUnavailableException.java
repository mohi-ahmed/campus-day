package com.spark.campusday.study.assistant.exception;

import com.spark.campusday.common.exception.ServiceUnavailableException;

public class TaskbotUnavailableException extends ServiceUnavailableException {
    public TaskbotUnavailableException(String message) {
        super(message);
    }
}
