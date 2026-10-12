package com.spark.campusday.marketplace.wanted.dto;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import com.spark.campusday.marketplace.wanted.model.WantedPostStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WantedPostDto(
        String id,
        String authorStudentId,
        String authorName,
        String title,
        String description,
        MarketplaceCategory category,
        BigDecimal maximumBudget,
        MarketplaceCondition acceptableCondition,
        MarketplacePickupZone pickupZone,
        WantedPostStatus status,
        long responseCount,
        Instant createdAt,
        Instant updatedAt
) {
}
