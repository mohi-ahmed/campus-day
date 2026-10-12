package com.spark.campusday.marketplace.wanted.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.marketplace.config.MarketplaceCacheNames;
import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceListing;
import com.spark.campusday.marketplace.listing.service.MarketplaceListingService;
import com.spark.campusday.marketplace.wanted.dto.WantedPostDto;
import com.spark.campusday.marketplace.wanted.dto.WantedPostPageResponse;
import com.spark.campusday.marketplace.wanted.dto.WantedPostRequest;
import com.spark.campusday.marketplace.wanted.dto.WantedResponseDto;
import com.spark.campusday.marketplace.wanted.dto.WantedResponseRequest;
import com.spark.campusday.marketplace.wanted.model.WantedPost;
import com.spark.campusday.marketplace.wanted.model.WantedPostResponse;
import com.spark.campusday.marketplace.wanted.model.WantedPostStatus;
import com.spark.campusday.marketplace.wanted.repository.WantedPostRepository;
import com.spark.campusday.marketplace.wanted.repository.WantedPostResponseRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WantedPostService {
    private final WantedPostRepository posts;
    private final WantedPostResponseRepository responses;
    private final StudentService students;
    private final MarketplaceListingService listings;

    @CacheEvict(cacheNames = MarketplaceCacheNames.WANTED_FEED, allEntries = true)
    public WantedPostDto create(WantedPostRequest request) {
        Student author = students.getByStudentId(request.authorStudentId().trim());
        WantedPost post = WantedPost.builder()
                .authorStudentId(author.getStudentId())
                .authorName(author.getName())
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .maximumBudget(request.maximumBudget())
                .acceptableCondition(request.acceptableCondition())
                .pickupZone(request.pickupZone())
                .build();
        return dto(posts.save(post));
    }

    @Cacheable(cacheNames = MarketplaceCacheNames.WANTED_FEED, sync = true)
    public WantedPostPageResponse list(
            MarketplaceCategory category,
            String query,
            int page,
            int size) {
        validatePage(page, size);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery != null && normalizedQuery.length() > 100) {
            throw new InvalidRequestException("Search text cannot exceed 100 characters");
        }
        Page<WantedPost> result = posts.search(
                WantedPostStatus.OPEN, category, normalizedQuery,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return new WantedPostPageResponse(
                result.getContent().stream().map(WantedPostService::dto).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Cacheable(cacheNames = MarketplaceCacheNames.WANTED_POSTS, key = "#postId", sync = true)
    public WantedPostDto get(String postId) {
        return dto(requireOpen(postId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MarketplaceCacheNames.WANTED_POSTS, key = "#postId"),
            @CacheEvict(cacheNames = MarketplaceCacheNames.WANTED_FEED, allEntries = true)
    })
    public WantedResponseDto respond(String postId, WantedResponseRequest request) {
        WantedPost post = requireOpen(postId);
        Student seller = students.getByStudentId(request.sellerStudentId().trim());
        if (post.getAuthorStudentId().equals(seller.getStudentId())) {
            throw new InvalidRequestException("You cannot respond to your own wanted post");
        }
        String listingId = request.listingId() == null || request.listingId().isBlank()
                ? null : request.listingId().trim();
        if (listingId != null) {
            MarketplaceListing listing = listings.requireAvailable(listingId);
            if (!listing.getSellerStudentId().equals(seller.getStudentId())) {
                throw new InvalidRequestException("The linked listing does not belong to this seller");
            }
        }
        WantedPostResponse response = WantedPostResponse.builder()
                .wantedPostId(postId)
                .sellerStudentId(seller.getStudentId())
                .sellerName(seller.getName())
                .listingId(listingId)
                .price(request.price())
                .message(request.message().trim())
                .build();
        try {
            WantedPostResponse saved = responses.save(response);
            posts.save(post.toBuilder().responseCount(post.getResponseCount() + 1).build());
            return dto(saved);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("You already responded to this wanted post", exception);
        }
    }

    public List<WantedResponseDto> listResponses(String postId, String authorStudentId) {
        WantedPost post = requireOpen(postId);
        if (!post.getAuthorStudentId().equals(authorStudentId.trim())) {
            throw new ResourceNotFoundException("Wanted post not found for this author: " + postId);
        }
        return responses.findAllByWantedPostIdOrderByCreatedAtDesc(postId).stream()
                .map(WantedPostService::dto).toList();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MarketplaceCacheNames.WANTED_POSTS, key = "#postId"),
            @CacheEvict(cacheNames = MarketplaceCacheNames.WANTED_FEED, allEntries = true)
    })
    public WantedPostDto close(String postId, String authorStudentId) {
        WantedPost post = requireOpen(postId);
        if (!post.getAuthorStudentId().equals(authorStudentId.trim())) {
            throw new ResourceNotFoundException("Wanted post not found for this author: " + postId);
        }
        return dto(posts.save(post.toBuilder().status(WantedPostStatus.CLOSED).build()));
    }

    private WantedPost requireOpen(String postId) {
        return posts.findByIdAndStatus(postId, WantedPostStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Open wanted post not found: " + postId));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new InvalidRequestException("Page must be at least 0 and size must be between 1 and 50");
        }
    }

    private static WantedPostDto dto(WantedPost post) {
        return new WantedPostDto(
                post.getId(), post.getAuthorStudentId(), post.getAuthorName(), post.getTitle(),
                post.getDescription(), post.getCategory(), post.getMaximumBudget(),
                post.getAcceptableCondition(), post.getPickupZone(), post.getStatus(),
                post.getResponseCount(), post.getCreatedAt(), post.getUpdatedAt());
    }

    private static WantedResponseDto dto(WantedPostResponse response) {
        return new WantedResponseDto(
                response.getId(), response.getWantedPostId(), response.getSellerStudentId(),
                response.getSellerName(), response.getListingId(), response.getPrice(),
                response.getMessage(), response.getStatus(), response.getCreatedAt());
    }
}
