package com.spark.campusday.marketplace.listing.dto;

import com.spark.campusday.marketplace.listing.model.MarketplaceListingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarketplaceListingStatusRequest(
        @NotBlank @Size(max = 64) String sellerStudentId,
        @NotNull MarketplaceListingStatus status
) {
}
