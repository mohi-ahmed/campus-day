package com.spark.campusday.marketplace.listing.repository;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplaceListing;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingStatus;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingType;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class MarketplaceListingSearchRepositoryImpl implements MarketplaceListingSearchRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<MarketplaceListing> search(
            MarketplaceListingStatus status,
            MarketplaceCategory category,
            MarketplaceCondition condition,
            MarketplaceListingType listingType,
            MarketplacePickupZone pickupZone,
            Boolean negotiable,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String searchText,
            Pageable pageable) {
        Query countQuery = query(status, category, condition, listingType, pickupZone,
                negotiable, minimumPrice, maximumPrice, searchText);
        long total = mongoTemplate.count(countQuery, MarketplaceListing.class);
        Query contentQuery = query(status, category, condition, listingType, pickupZone,
                negotiable, minimumPrice, maximumPrice, searchText).with(pageable);
        return new PageImpl<>(mongoTemplate.find(contentQuery, MarketplaceListing.class), pageable, total);
    }

    private Query query(
            MarketplaceListingStatus status,
            MarketplaceCategory category,
            MarketplaceCondition condition,
            MarketplaceListingType listingType,
            MarketplacePickupZone pickupZone,
            Boolean negotiable,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String searchText) {
        Query query = Query.query(Criteria.where("status").is(status));
        if (category != null) query.addCriteria(Criteria.where("category").is(category));
        if (condition != null) query.addCriteria(Criteria.where("condition").is(condition));
        if (listingType != null) query.addCriteria(Criteria.where("listingType").is(listingType));
        if (pickupZone != null) query.addCriteria(Criteria.where("pickupZone").is(pickupZone));
        if (negotiable != null) query.addCriteria(Criteria.where("negotiable").is(negotiable));
        if (minimumPrice != null || maximumPrice != null) {
            Criteria price = Criteria.where("price");
            if (minimumPrice != null) price.gte(minimumPrice);
            if (maximumPrice != null) price.lte(maximumPrice);
            query.addCriteria(price);
        }
        if (searchText != null && !searchText.isBlank()) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matching(searchText.trim()));
        }
        return query;
    }
}
