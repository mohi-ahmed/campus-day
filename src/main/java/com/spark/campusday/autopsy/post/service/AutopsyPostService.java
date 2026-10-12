package com.spark.campusday.autopsy.post.service;

import com.spark.campusday.autopsy.post.dto.PostPage;
import com.spark.campusday.autopsy.post.dto.PostRequest;
import com.spark.campusday.autopsy.post.dto.PostResponse;
import com.spark.campusday.autopsy.post.dto.ViewResponse;
import com.spark.campusday.autopsy.post.model.AutopsyCategory;
import com.spark.campusday.autopsy.post.model.AutopsyPost;
import com.spark.campusday.autopsy.post.model.AutopsyPostStatus;
import com.spark.campusday.autopsy.post.repository.AutopsyPostRepository;
import com.spark.campusday.autopsy.support.AutopsyInput;
import com.spark.campusday.autopsy.support.AnonymousActorFingerprint;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutopsyPostService {
    public static final String FEED_CACHE = "autopsyFeed";
    public static final String POST_CACHE = "autopsyPosts";

    private static final List<String> REACTION_NAMES = List.of(
            "LIKE", "UNLIKE", "LOVE", "BROKEN_HEART", "HAHA",
            "WOW", "ANGRY", "CRY", "FIRE");
    private final AutopsyPostRepository posts;
    private final MongoTemplate mongoTemplate;
    private final FlagRateLimiter flagRateLimiter;
    private final PostCreationRateLimit postCreationRateLimit;
    private final AnonymousActorFingerprint actorFingerprint;

    @CacheEvict(cacheNames = FEED_CACHE, allEntries = true)
    public PostResponse create(PostRequest request, String clientIp, String userAgent) {
        String actorKey = actorFingerprint.create(request.sessionId(), clientIp, userAgent);
        return flagRateLimiter.execute(actorKey,
                () -> postCreationRateLimit.execute(actorKey, () -> save(request)));
    }

    private PostResponse save(PostRequest request) {
        AutopsyPost post = AutopsyPost.builder()
                .category(request.category())
                .toWhom(validateToWhom(request.category(), request.toWhom()))
                .content(request.thoughts().trim())
                .nickname(AutopsyInput.nickname(request.nickname()))
                .tag(AutopsyInput.tag(request.tag()))
                .reactionCounts(emptyReactionCounts())
                .build();
        return AutopsyPostMapper.response(posts.save(post));
    }

    @Cacheable(
            cacheNames = FEED_CACHE,
            key = "(#category == null ? 'ALL' : #category.name()) + ':' + #page + ':' + #size",
            sync = true)
    public PostPage list(AutopsyCategory category, int page, int size) {
        AutopsyInput.validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<AutopsyPost> result = category == null
                ? posts.findByStatus(AutopsyPostStatus.ACTIVE, pageable)
                : posts.findByCategoryAndStatus(category, AutopsyPostStatus.ACTIVE, pageable);
        return new PostPage(
                result.getContent().stream().map(AutopsyPostMapper::response).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Cacheable(cacheNames = POST_CACHE, key = "#postId", sync = true)
    public PostResponse get(String postId) {
        return AutopsyPostMapper.response(requireActive(postId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = POST_CACHE, key = "#postId"),
            @CacheEvict(cacheNames = FEED_CACHE, allEntries = true)
    })
    public ViewResponse recordView(String postId) {
        AutopsyPost updated = updateActivePost(postId, new Update().inc("viewCount", 1));
        return new ViewResponse(postId, updated.getViewCount());
    }

    public AutopsyPost requireActive(String postId) {
        return posts.findByIdAndStatus(postId, AutopsyPostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Autopsy post not found: " + postId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = POST_CACHE, key = "#postId"),
            @CacheEvict(cacheNames = FEED_CACHE, allEntries = true)
    })
    public AutopsyPost incrementCommentCount(String postId) {
        return updateActivePost(postId, new Update().inc("commentCount", 1));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = POST_CACHE, key = "#postId"),
            @CacheEvict(cacheNames = FEED_CACHE, allEntries = true)
    })
    public AutopsyPost applyReactionChange(String postId, String previous, String selected) {
        Update update = new Update();
        if (previous != null) {
            update.inc("reactionCounts." + previous, -1);
        }
        if (selected != null) {
            update.inc("reactionCounts." + selected, 1);
        }
        if (previous == null && selected != null) {
            update.inc("totalReactionCount", 1);
        } else if (previous != null && selected == null) {
            update.inc("totalReactionCount", -1);
        }
        return updateActivePost(postId, update);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = POST_CACHE, key = "#postId"),
            @CacheEvict(cacheNames = FEED_CACHE, allEntries = true)
    })
    public AutopsyPost updateRepostCount(String postId, boolean reposted) {
        return updateActivePost(postId, new Update().inc("repostCount", reposted ? 1 : -1));
    }

    private AutopsyPost updateActivePost(String postId, Update update) {
        Query query = Query.query(Criteria.where("_id").is(postId)
                .and("status").is(AutopsyPostStatus.ACTIVE));
        AutopsyPost updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                AutopsyPost.class);
        if (updated == null) {
            throw new ResourceNotFoundException("Autopsy post not found: " + postId);
        }
        return updated;
    }

    private String validateToWhom(AutopsyCategory category, String value) {
        String normalized = AutopsyInput.optional(value);
        if (category == AutopsyCategory.COMPLAINT && (normalized == null || normalized.length() < 2)) {
            throw new InvalidRequestException(
                    "Complaint posts require a To whom value of at least 2 characters");
        }
        if (category == AutopsyCategory.ROASTING && normalized != null && normalized.length() < 2) {
            throw new InvalidRequestException("Roasting To whom value must contain at least 2 characters");
        }
        return category == AutopsyCategory.COMPLAINT || category == AutopsyCategory.ROASTING
                ? normalized
                : null;
    }

    private Map<String, Long> emptyReactionCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        REACTION_NAMES.forEach(name -> counts.put(name, 0L));
        return counts;
    }
}
