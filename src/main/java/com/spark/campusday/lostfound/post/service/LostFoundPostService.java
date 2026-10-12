package com.spark.campusday.lostfound.post.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.lostfound.config.LostFoundCacheNames;
import com.spark.campusday.lostfound.post.dto.LostFoundPostPageResponse;
import com.spark.campusday.lostfound.post.dto.LostFoundPostRequest;
import com.spark.campusday.lostfound.post.dto.LostFoundPostResponse;
import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundPost;
import com.spark.campusday.lostfound.post.model.LostFoundPostStatus;
import com.spark.campusday.lostfound.post.model.LostFoundType;
import com.spark.campusday.lostfound.post.repository.LostFoundPostRepository;
import com.spark.campusday.lostfound.support.LostFoundInput;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LostFoundPostService {
    private final LostFoundPostRepository posts;
    private final StudentService students;
    private final MongoTemplate mongoTemplate;

    @CacheEvict(cacheNames = LostFoundCacheNames.FEED, allEntries = true)
    public LostFoundPostResponse create(LostFoundPostRequest request) {
        if (request.occurredOn().isAfter(LocalDate.now())) {
            throw new InvalidRequestException("Item date cannot be in the future");
        }
        Student owner = students.getByStudentId(request.ownerStudentId().trim());
        LostFoundPost post = LostFoundPost.builder()
                .ownerStudentId(owner.getStudentId())
                .ownerName(owner.getName())
                .contact(LostFoundInput.optional(request.contact()))
                .type(request.type())
                .category(request.category())
                .title(request.title().trim())
                .description(LostFoundInput.optional(request.description()))
                .location(request.location().trim())
                .occurredOn(request.occurredOn())
                .build();
        return LostFoundPostMapper.response(posts.save(post));
    }

    @Cacheable(cacheNames = LostFoundCacheNames.FEED, sync = true)
    public LostFoundPostPageResponse list(
            LostFoundType type,
            LostFoundCategory category,
            String searchText,
            int page,
            int size) {
        LostFoundInput.validatePage(page, size);
        String normalizedSearch = LostFoundInput.optional(searchText);
        if (normalizedSearch != null && normalizedSearch.length() > 100) {
            throw new InvalidRequestException("Search text cannot exceed 100 characters");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<LostFoundPost> result = posts.search(
                LostFoundPostStatus.OPEN, type, category, normalizedSearch, pageable);
        return new LostFoundPostPageResponse(
                result.getContent().stream().map(LostFoundPostMapper::response).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Cacheable(cacheNames = LostFoundCacheNames.POSTS, key = "#postId", sync = true)
    public LostFoundPostResponse get(String postId) {
        return LostFoundPostMapper.response(requireOpen(postId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LostFoundCacheNames.POSTS, key = "#postId"),
            @CacheEvict(cacheNames = LostFoundCacheNames.FEED, allEntries = true),
            @CacheEvict(cacheNames = LostFoundCacheNames.COMMENTS, allEntries = true)
    })
    public LostFoundPostResponse markReturned(String postId, String ownerStudentId) {
        Student owner = students.getByStudentId(ownerStudentId.trim());
        Query query = Query.query(Criteria.where("_id").is(postId)
                .and("ownerStudentId").is(owner.getStudentId())
                .and("status").is(LostFoundPostStatus.OPEN));
        Instant now = Instant.now();
        LostFoundPost updated = mongoTemplate.findAndModify(
                query,
                new Update()
                        .set("status", LostFoundPostStatus.RETURNED)
                        .set("returnedAt", now)
                        .set("updatedAt", now),
                FindAndModifyOptions.options().returnNew(true),
                LostFoundPost.class);
        if (updated == null) {
            throw new ResourceNotFoundException("Open lost and found post not found for this owner: " + postId);
        }
        return LostFoundPostMapper.response(updated);
    }

    public LostFoundPost requireOpen(String postId) {
        return posts.findByIdAndStatus(postId, LostFoundPostStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Open lost and found post not found: " + postId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LostFoundCacheNames.POSTS, key = "#postId"),
            @CacheEvict(cacheNames = LostFoundCacheNames.FEED, allEntries = true)
    })
    public void incrementCommentCount(String postId) {
        Query query = Query.query(Criteria.where("_id").is(postId)
                .and("status").is(LostFoundPostStatus.OPEN));
        LostFoundPost updated = mongoTemplate.findAndModify(
                query,
                new Update().inc("commentCount", 1).set("updatedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                LostFoundPost.class);
        if (updated == null) {
            throw new ResourceNotFoundException("Open lost and found post not found: " + postId);
        }
    }

    public Map<String, LostFoundPost> findByIds(Collection<String> postIds) {
        return posts.findAllById(postIds).stream()
                .collect(Collectors.toMap(LostFoundPost::getId, Function.identity()));
    }
}
