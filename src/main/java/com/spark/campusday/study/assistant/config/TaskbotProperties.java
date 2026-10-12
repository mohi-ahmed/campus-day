package com.spark.campusday.study.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.study.taskbot")
public record TaskbotProperties(
        String apiKey,
        String model,
        String groqApiKey,
        String groqBaseUrl,
        String groqModel,
        double temperature,
        int maxOutputTokens,
        int requestsPerMinute,
        int maxContextTasks,
        int maxTopicsPerSubject
) {
    public boolean configured() {
        return geminiConfigured() || groqConfigured();
    }

    public boolean geminiConfigured() {
        return hasText(apiKey);
    }

    public boolean groqConfigured() {
        return hasText(groqApiKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
