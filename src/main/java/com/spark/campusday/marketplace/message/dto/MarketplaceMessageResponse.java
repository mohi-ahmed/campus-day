package com.spark.campusday.marketplace.message.dto;

import java.time.Instant;

public record MarketplaceMessageResponse(
        String id,
        String senderStudentId,
        String senderName,
        String recipientStudentId,
        String recipientName,
        String listingId,
        String listingTitle,
        String body,
        Instant createdAt
) {
}
