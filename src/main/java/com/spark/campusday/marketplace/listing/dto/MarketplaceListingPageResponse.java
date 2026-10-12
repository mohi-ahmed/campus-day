package com.spark.campusday.marketplace.listing.dto;

import java.util.List;

public record MarketplaceListingPageResponse(
        List<MarketplaceListingResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
