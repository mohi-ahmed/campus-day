package com.spark.campusday.marketplace.image.dto;

import java.time.Instant;

public record MarketplaceImageResponse(
        String id,
        String secureUrl,
        String format,
        int width,
        int height,
        long bytes,
        int sortOrder,
        Instant createdAt
) {
}
