package com.spark.campusday.marketplace.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarketplaceCacheNames {
    public static final String LISTING_FEED = "marketplaceListingFeed";
    public static final String LISTINGS = "marketplaceListings";
    public static final String WANTED_FEED = "marketplaceWantedFeed";
    public static final String WANTED_POSTS = "marketplaceWantedPosts";
    public static final String FAVORITES = "marketplaceFavorites";
}
