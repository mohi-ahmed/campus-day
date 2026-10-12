package com.spark.campusday.marketplace.listing.dto;

import com.spark.campusday.marketplace.image.dto.MarketplaceImageResponse;
import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingStatus;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingType;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MarketplaceListingResponse(
        String id,
        String sellerStudentId,
        String sellerName,
        String title,
        String description,
        MarketplaceCategory category,
        BigDecimal price,
        MarketplaceCondition condition,
        MarketplaceListingType listingType,
        int quantity,
        boolean negotiable,
        MarketplacePickupZone pickupZone,
        MarketplaceListingStatus status,
        List<MarketplaceImageResponse> images,
        long viewCount,
        Instant createdAt,
        Instant updatedAt
) {
}
