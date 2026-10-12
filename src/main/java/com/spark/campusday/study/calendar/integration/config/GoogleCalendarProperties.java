package com.spark.campusday.study.calendar.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.study.calendar.google")
public record GoogleCalendarProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationUri,
        String tokenUri,
        String calendarApiBaseUri
) {
    public boolean configured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }
}
