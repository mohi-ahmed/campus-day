package com.spark.campusday.marketplace.listing.dto;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingType;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MarketplaceListingUpdateRequest(
        @NotBlank @Size(max = 64) String sellerStudentId,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotNull MarketplaceCategory category,
        @NotNull @DecimalMin(value = "1.00") BigDecimal price,
        @NotNull MarketplaceCondition condition,
        @NotNull MarketplaceListingType listingType,
        @Min(1) @Max(1000) int quantity,
        boolean negotiable,
        @NotNull MarketplacePickupZone pickupZone
) {
}
