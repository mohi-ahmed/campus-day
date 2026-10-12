package com.spark.campusday.lostfound.post.repository;

import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundPost;
import com.spark.campusday.lostfound.post.model.LostFoundPostStatus;
import com.spark.campusday.lostfound.post.model.LostFoundType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;

@RequiredArgsConstructor
public class LostFoundPostSearchRepositoryImpl implements LostFoundPostSearchRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<LostFoundPost> search(
            LostFoundPostStatus status,
            LostFoundType type,
            LostFoundCategory category,
            String searchText,
            Pageable pageable) {
        Query countQuery = query(status, type, category, searchText);
        long total = mongoTemplate.count(countQuery, LostFoundPost.class);

        Query contentQuery = query(status, type, category, searchText).with(pageable);
        return new PageImpl<>(
                mongoTemplate.find(contentQuery, LostFoundPost.class),
                pageable,
                total);
    }

    private Query query(
            LostFoundPostStatus status,
            LostFoundType type,
            LostFoundCategory category,
            String searchText) {
        Query query = Query.query(Criteria.where("status").is(status));
        if (type != null) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (category != null) {
            query.addCriteria(Criteria.where("category").is(category));
        }
        if (searchText != null && !searchText.isBlank()) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matching(searchText.trim()));
        }
        return query;
    }
}
