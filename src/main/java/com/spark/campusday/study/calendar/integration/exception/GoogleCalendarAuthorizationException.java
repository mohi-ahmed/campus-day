package com.spark.campusday.study.calendar.integration.exception;

public class GoogleCalendarAuthorizationException
        extends CalendarIntegrationException {
    private final boolean reconnectRequired;

    public GoogleCalendarAuthorizationException(
            String message, boolean reconnectRequired) {
        super(message);
        this.reconnectRequired = reconnectRequired;
    }

    public boolean reconnectRequired() {
        return reconnectRequired;
    }
}
