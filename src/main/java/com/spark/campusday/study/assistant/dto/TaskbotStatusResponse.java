package com.spark.campusday.study.assistant.dto;

public record TaskbotStatusResponse(
        boolean available,
        String provider,
        String model
) {
}
