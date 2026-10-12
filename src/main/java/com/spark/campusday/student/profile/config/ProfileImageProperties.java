package com.spark.campusday.student.profile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.student.profile-image")
public record ProfileImageProperties(
        String assetFolder,
        DataSize maxFileSize
) {
}
