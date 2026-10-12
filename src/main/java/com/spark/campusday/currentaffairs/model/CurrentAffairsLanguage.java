package com.spark.campusday.currentaffairs.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.spark.campusday.common.exception.InvalidRequestException;

import java.util.Arrays;

public enum CurrentAffairsLanguage {
    BENGALI("bn"),
    ENGLISH("en");

    private final String value;

    CurrentAffairsLanguage(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static CurrentAffairsLanguage parse(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(values())
                .filter(language -> language.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Unsupported current affairs language: " + value));
    }
}
