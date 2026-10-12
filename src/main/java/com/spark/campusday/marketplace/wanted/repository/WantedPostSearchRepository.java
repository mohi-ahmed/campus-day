package com.spark.campusday.marketplace.wanted.repository;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.wanted.model.WantedPost;
import com.spark.campusday.marketplace.wanted.model.WantedPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WantedPostSearchRepository {
    Page<WantedPost> search(
            WantedPostStatus status,
            MarketplaceCategory category,
            String searchText,
            Pageable pageable);
}
