package com.spark.campusday.marketplace.offer.repository;

import com.spark.campusday.marketplace.offer.model.MarketplaceOffer;
import com.spark.campusday.marketplace.offer.model.MarketplaceOfferStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface MarketplaceOfferRepository extends MongoRepository<MarketplaceOffer, String> {
    boolean existsByListingIdAndBuyerStudentIdAndStatusIn(
            String listingId,
            String buyerStudentId,
            Collection<MarketplaceOfferStatus> statuses);

    List<MarketplaceOffer> findAllBySellerStudentIdOrderByCreatedAtDesc(String sellerStudentId);
    List<MarketplaceOffer> findAllByBuyerStudentIdOrderByCreatedAtDesc(String buyerStudentId);
    List<MarketplaceOffer> findAllByListingIdAndStatusIn(
            String listingId,
            Collection<MarketplaceOfferStatus> statuses);
}
