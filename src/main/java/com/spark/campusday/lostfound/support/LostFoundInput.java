package com.spark.campusday.lostfound.support;

import com.spark.campusday.common.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LostFoundInput {
    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new InvalidRequestException("Page must be at least 0 and size must be between 1 and 50");
        }
    }

    public static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
