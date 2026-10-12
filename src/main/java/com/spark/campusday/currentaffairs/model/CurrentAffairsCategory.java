package com.spark.campusday.currentaffairs.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.spark.campusday.common.exception.InvalidRequestException;

import java.util.Arrays;

public enum CurrentAffairsCategory {
    BANGLADESH("Bangladesh"),
    INTERNATIONAL("International"),
    EDUCATION("Education"),
    SCIENCE("Science"),
    TECHNOLOGY("Technology"),
    HEALTH("Health"),
    BUSINESS("Business"),
    SPORTS("Sports"),
    POLITICS("Politics"),
    ENVIRONMENT("Environment");

    private final String value;

    CurrentAffairsCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static CurrentAffairsCategory parse(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(values())
                .filter(category -> category.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Unsupported current affairs category: " + value));
    }
}
