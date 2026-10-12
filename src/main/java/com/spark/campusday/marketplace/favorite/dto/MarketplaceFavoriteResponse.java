package com.spark.campusday.marketplace.favorite.dto;

import com.spark.campusday.marketplace.listing.dto.MarketplaceListingResponse;

import java.util.List;

public record MarketplaceFavoriteResponse(
        String studentId,
        List<MarketplaceListingResponse> items
) {
}
