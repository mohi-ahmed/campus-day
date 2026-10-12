package com.spark.campusday.currentaffairs.model;

import com.spark.campusday.common.exception.InvalidRequestException;

public enum CurrentAffairsContentType {
    CURRENT_AFFAIRS,
    NEWS;

    public static CurrentAffairsContentType parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Unsupported current affairs content type: " + value);
        }
    }
}
