package com.spark.campusday.study.assistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TaskbotProperties.class)
public class TaskbotConfiguration {
}
