package com.spark.campusday.study.calendar.integration.dto;

import java.time.Instant;

public record GoogleTokenResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
}
