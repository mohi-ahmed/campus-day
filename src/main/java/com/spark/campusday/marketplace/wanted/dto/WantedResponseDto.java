package com.spark.campusday.marketplace.wanted.dto;

import com.spark.campusday.marketplace.wanted.model.WantedResponseStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WantedResponseDto(
        String id,
        String wantedPostId,
        String sellerStudentId,
        String sellerName,
        String listingId,
        BigDecimal price,
        String message,
        WantedResponseStatus status,
        Instant createdAt
) {
}
