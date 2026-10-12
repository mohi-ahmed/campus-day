package com.spark.campusday.marketplace.favorite.repository;

import com.spark.campusday.marketplace.favorite.model.MarketplaceFavorite;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceFavoriteRepository extends MongoRepository<MarketplaceFavorite, String> {
    List<MarketplaceFavorite> findAllByStudentIdOrderByCreatedAtDesc(String studentId);
    Optional<MarketplaceFavorite> findByStudentIdAndListingId(String studentId, String listingId);
}
