package com.spark.campusday.marketplace.wanted.dto;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WantedPostRequest(
        @NotBlank @Size(max = 64) String authorStudentId,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull MarketplaceCategory category,
        @NotNull @DecimalMin("1.00") BigDecimal maximumBudget,
        @NotNull MarketplaceCondition acceptableCondition,
        @NotNull MarketplacePickupZone pickupZone
) {
}
