package com.spark.campusday.marketplace.wanted.repository;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.wanted.model.WantedPost;
import com.spark.campusday.marketplace.wanted.model.WantedPostStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;

@RequiredArgsConstructor
public class WantedPostSearchRepositoryImpl implements WantedPostSearchRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<WantedPost> search(
            WantedPostStatus status,
            MarketplaceCategory category,
            String searchText,
            Pageable pageable) {
        Query countQuery = query(status, category, searchText);
        long total = mongoTemplate.count(countQuery, WantedPost.class);
        Query contentQuery = query(status, category, searchText).with(pageable);
        return new PageImpl<>(mongoTemplate.find(contentQuery, WantedPost.class), pageable, total);
    }

    private Query query(WantedPostStatus status, MarketplaceCategory category, String searchText) {
        Query query = Query.query(Criteria.where("status").is(status));
        if (category != null) query.addCriteria(Criteria.where("category").is(category));
        if (searchText != null && !searchText.isBlank()) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matching(searchText.trim()));
        }
        return query;
    }
}
