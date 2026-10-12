package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.currentaffairs.config.CurrentAffairsConfiguration;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsFilter;
import com.spark.campusday.currentaffairs.model.CurrentAffairsArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CurrentAffairsQueryService {
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("publishedAt"),
            Sort.Order.desc("id"));

    private final MongoTemplate mongoTemplate;

    @Cacheable(cacheNames = CurrentAffairsConfiguration.FEED_CACHE, sync = true)
    public Page<CurrentAffairsArticle> searchShared(
            CurrentAffairsFilter filter,
            int page,
            int size) {
        return search(filter, null, Set.of(), page, size);
    }

    public Page<CurrentAffairsArticle> searchPersonalized(
            CurrentAffairsFilter filter,
            Set<String> includedArticleIds,
            Set<String> excludedArticleIds,
            int page,
            int size) {
        return search(filter, includedArticleIds, excludedArticleIds, page, size);
    }

    @Cacheable(cacheNames = CurrentAffairsConfiguration.ARTICLE_CACHE, key = "#articleId", sync = true)
    public CurrentAffairsArticle getArticle(String articleId) {
        return mongoTemplate.findById(articleId, CurrentAffairsArticle.class);
    }

    public CurrentAffairsArticle requireArticle(String articleId) {
        CurrentAffairsArticle article = getArticle(articleId);
        if (article == null) {
            throw new ResourceNotFoundException("Current affairs article not found: " + articleId);
        }
        return article;
    }

    @Cacheable(cacheNames = CurrentAffairsConfiguration.HIGHLIGHTS_CACHE, sync = true)
    public List<CurrentAffairsArticle> highlights() {
        Query query = new Query().with(NEWEST_FIRST).limit(3);
        return mongoTemplate.find(query, CurrentAffairsArticle.class);
    }

    private Page<CurrentAffairsArticle> search(
            CurrentAffairsFilter filter,
            Set<String> includedArticleIds,
            Set<String> excludedArticleIds,
            int page,
            int size) {
        PageRequest pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Query query = query(filter, includedArticleIds, excludedArticleIds);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CurrentAffairsArticle.class);
        List<CurrentAffairsArticle> content = mongoTemplate.find(query.with(pageable), CurrentAffairsArticle.class);
        return new PageImpl<>(content, pageable, total);
    }

    private Query query(
            CurrentAffairsFilter filter,
            Set<String> includedArticleIds,
            Set<String> excludedArticleIds) {
        Query query = new Query();
        if (filter.query() != null) {
            Pattern text = Pattern.compile(Pattern.quote(filter.query()), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(text),
                    Criteria.where("description").regex(text),
                    Criteria.where("sourceName").regex(text),
                    Criteria.where("keywords").regex(text)));
        }
        addEqual(query, "category", filter.category());
        addEqual(query, "region", filter.region());
        addEqual(query, "language", filter.language());
        addEqual(query, "sourceName", filter.source());
        addEqual(query, "contentType", filter.contentType());

        List<Criteria> idCriteria = new ArrayList<>();
        if (includedArticleIds != null) {
            idCriteria.add(Criteria.where("_id").in(includedArticleIds));
        }
        if (excludedArticleIds != null && !excludedArticleIds.isEmpty()) {
            idCriteria.add(Criteria.where("_id").nin(excludedArticleIds));
        }
        if (!idCriteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(idCriteria));
        }
        return query;
    }

    private static void addEqual(Query query, String field, Object value) {
        if (value != null) query.addCriteria(Criteria.where(field).is(value));
    }
}
