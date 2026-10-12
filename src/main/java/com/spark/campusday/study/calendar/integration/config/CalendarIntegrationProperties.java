package com.spark.campusday.study.calendar.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.study.calendar.integration")
public record CalendarIntegrationProperties(
        String tokenEncryptionKey,
        Duration oauthStateTtl
) {
}
