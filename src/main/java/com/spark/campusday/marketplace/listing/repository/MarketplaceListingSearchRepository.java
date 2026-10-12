package com.spark.campusday.marketplace.listing.repository;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplaceListing;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingStatus;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingType;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface MarketplaceListingSearchRepository {
    Page<MarketplaceListing> search(
            MarketplaceListingStatus status,
            MarketplaceCategory category,
            MarketplaceCondition condition,
            MarketplaceListingType listingType,
            MarketplacePickupZone pickupZone,
            Boolean negotiable,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String searchText,
            Pageable pageable);
}
