package com.spark.campusday.currentaffairs.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.spark.campusday.common.exception.InvalidRequestException;

import java.util.Arrays;

public enum CurrentAffairsRegion {
    BANGLADESH("bangladesh"),
    INTERNATIONAL("international");

    private final String value;

    CurrentAffairsRegion(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static CurrentAffairsRegion parse(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(values())
                .filter(region -> region.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Unsupported current affairs region: " + value));
    }
}
