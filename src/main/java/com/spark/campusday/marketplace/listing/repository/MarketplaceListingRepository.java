package com.spark.campusday.marketplace.listing.repository;

import com.spark.campusday.marketplace.listing.model.MarketplaceListing;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MarketplaceListingRepository
        extends MongoRepository<MarketplaceListing, String>, MarketplaceListingSearchRepository {
    Optional<MarketplaceListing> findByIdAndStatusNot(String id, MarketplaceListingStatus status);

    Page<MarketplaceListing> findAllBySellerStudentIdAndStatusNot(
            String sellerStudentId,
            MarketplaceListingStatus status,
            Pageable pageable);

    List<MarketplaceListing> findAllByIdInAndStatusNot(
            Collection<String> ids,
            MarketplaceListingStatus status);
}
