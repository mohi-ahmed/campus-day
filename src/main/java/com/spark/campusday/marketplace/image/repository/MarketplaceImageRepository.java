package com.spark.campusday.marketplace.image.repository;

import com.spark.campusday.marketplace.image.model.MarketplaceImage;
import com.spark.campusday.marketplace.image.model.MarketplaceImageStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface MarketplaceImageRepository extends MongoRepository<MarketplaceImage, String> {
    List<MarketplaceImage> findAllByIdInAndUploaderStudentIdAndStatus(
            Collection<String> ids,
            String uploaderStudentId,
            MarketplaceImageStatus status);

    List<MarketplaceImage> findAllByListingIdOrderBySortOrder(String listingId);
}
