package com.spark.campusday.study.calendar.integration.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class CalendarIntegrationException extends InvalidBusinessStateException {
    public CalendarIntegrationException(String message) {
        super(message);
    }
}
