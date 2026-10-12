package com.spark.campusday.marketplace.listing.service;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.marketplace.config.MarketplaceCacheNames;
import com.spark.campusday.marketplace.image.model.MarketplaceImage;
import com.spark.campusday.marketplace.image.service.MarketplaceImageService;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingCreateRequest;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingPageResponse;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingResponse;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingStatusRequest;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingUpdateRequest;
import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplaceListing;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingStatus;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingType;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import com.spark.campusday.marketplace.listing.model.MarketplaceSort;
import com.spark.campusday.marketplace.listing.repository.MarketplaceListingRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceListingService {
    private final MarketplaceListingRepository listings;
    private final MarketplaceImageService images;
    private final StudentService students;

    @CacheEvict(cacheNames = MarketplaceCacheNames.LISTING_FEED, allEntries = true)
    public MarketplaceListingResponse create(MarketplaceListingCreateRequest request) {
        String sellerStudentId = request.sellerStudentId().trim();
        Student seller = students.getByStudentId(sellerStudentId);
        validateListingValues(request.listingType(), request.condition(), request.quantity(), request.price());
        List<MarketplaceImage> temporaryImages = images.requireTemporaryImages(request.imageIds(), sellerStudentId);

        MarketplaceListing listing = MarketplaceListing.builder()
                .sellerStudentId(seller.getStudentId())
                .sellerName(seller.getName())
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .price(request.price())
                .condition(request.condition())
                .listingType(request.listingType())
                .quantity(request.quantity())
                .negotiable(request.negotiable())
                .pickupZone(request.pickupZone())
                .imageIds(List.copyOf(request.imageIds()))
                .build();
        MarketplaceListing saved = listings.save(listing);
        List<MarketplaceImage> attached = images.attachToListing(temporaryImages, saved.getId());
        Map<String, MarketplaceImage> imageMap = attached.stream()
                .collect(Collectors.toMap(MarketplaceImage::getId, Function.identity()));
        return response(saved, imageMap);
    }

    @Cacheable(cacheNames = MarketplaceCacheNames.LISTING_FEED, sync = true)
    public MarketplaceListingPageResponse list(
            MarketplaceCategory category,
            MarketplaceCondition condition,
            MarketplaceListingType listingType,
            MarketplacePickupZone pickupZone,
            Boolean negotiable,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String query,
            MarketplaceSort sort,
            int page,
            int size) {
        validatePageAndPrices(page, size, minimumPrice, maximumPrice);
        String normalizedQuery = optional(query);
        if (normalizedQuery != null && normalizedQuery.length() > 100) {
            throw new InvalidRequestException("Search text cannot exceed 100 characters");
        }
        PageRequest pageable = PageRequest.of(page, size, sort(sort));
        Page<MarketplaceListing> result = listings.search(
                MarketplaceListingStatus.AVAILABLE, category, condition, listingType,
                pickupZone, negotiable, minimumPrice, maximumPrice, normalizedQuery, pageable);
        return pageResponse(result, page, size);
    }

    @Cacheable(cacheNames = MarketplaceCacheNames.LISTINGS, key = "#listingId", sync = true)
    public MarketplaceListingResponse get(String listingId) {
        MarketplaceListing listing = requireVisible(listingId);
        return response(listing, images.findByIds(listing.getImageIds()));
    }

    @Cacheable(cacheNames = MarketplaceCacheNames.LISTING_FEED, sync = true)
    public MarketplaceListingPageResponse listSellerListings(String studentId, int page, int size) {
        validatePageAndPrices(page, size, null, null);
        students.getByStudentId(studentId);
        Page<MarketplaceListing> result = listings.findAllBySellerStudentIdAndStatusNot(
                studentId, MarketplaceListingStatus.UNAVAILABLE,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return pageResponse(result, page, size);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MarketplaceCacheNames.LISTINGS, key = "#listingId"),
            @CacheEvict(cacheNames = MarketplaceCacheNames.LISTING_FEED, allEntries = true),
            @CacheEvict(cacheNames = MarketplaceCacheNames.FAVORITES, allEntries = true)
    })
    public MarketplaceListingResponse update(
            String listingId,
            MarketplaceListingUpdateRequest request) {
        MarketplaceListing current = requireOwned(listingId, request.sellerStudentId());
        if (current.getStatus() == MarketplaceListingStatus.SOLD) {
            throw new InvalidBusinessStateException("Sold listings cannot be edited");
        }
        validateListingValues(request.listingType(), request.condition(), request.quantity(), request.price());
        MarketplaceListing updated = current.toBuilder()
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .price(request.price())
                .condition(request.condition())
                .listingType(request.listingType())
                .quantity(request.quantity())
                .negotiable(request.negotiable())
                .pickupZone(request.pickupZone())
                .build();
        MarketplaceListing saved = listings.save(updated);
        return response(saved, images.findByIds(saved.getImageIds()));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MarketplaceCacheNames.LISTINGS, key = "#listingId"),
            @CacheEvict(cacheNames = MarketplaceCacheNames.LISTING_FEED, allEntries = true),
            @CacheEvict(cacheNames = MarketplaceCacheNames.FAVORITES, allEntries = true)
    })
    public MarketplaceListingResponse changeStatus(
            String listingId,
            MarketplaceListingStatusRequest request) {
        MarketplaceListing current = requireOwned(listingId, request.sellerStudentId());
        if (request.status() == MarketplaceListingStatus.RESERVED) {
            throw new InvalidRequestException("A listing becomes reserved only after an offer is accepted");
        }
        MarketplaceListing saved = listings.save(current.toBuilder().status(request.status()).build());
        return response(saved, images.findByIds(saved.getImageIds()));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MarketplaceCacheNames.LISTINGS, key = "#listingId"),
            @CacheEvict(cacheNames = MarketplaceCacheNames.LISTING_FEED, allEntries = true),
            @CacheEvict(cacheNames = MarketplaceCacheNames.FAVORITES, allEntries = true)
    })
    public MarketplaceListing reserve(String listingId, String sellerStudentId) {
        MarketplaceListing listing = requireOwned(listingId, sellerStudentId);
        if (listing.getStatus() != MarketplaceListingStatus.AVAILABLE) {
            throw new InvalidBusinessStateException("Only available listings can be reserved");
        }
        return listings.save(listing.toBuilder().status(MarketplaceListingStatus.RESERVED).build());
    }

    public MarketplaceListing requireAvailable(String listingId) {
        return listings.findById(listingId)
                .filter(item -> item.getStatus() == MarketplaceListingStatus.AVAILABLE)
                .orElseThrow(() -> new ResourceNotFoundException("Available marketplace listing not found: " + listingId));
    }

    public List<MarketplaceListingResponse> findVisibleByIds(Collection<String> ids) {
        List<MarketplaceListing> found = listings.findAllByIdInAndStatusNot(ids, MarketplaceListingStatus.UNAVAILABLE);
        Map<String, MarketplaceListing> byId = found.stream()
                .collect(Collectors.toMap(MarketplaceListing::getId, Function.identity()));
        Map<String, MarketplaceImage> imageMap = images.findByIds(
                found.stream().flatMap(item -> item.getImageIds().stream()).distinct().toList());
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .map(item -> response(item, imageMap)).toList();
    }

    private MarketplaceListingPageResponse pageResponse(Page<MarketplaceListing> pageResult, int page, int size) {
        Map<String, MarketplaceImage> imageMap = images.findByIds(pageResult.getContent().stream()
                .flatMap(item -> item.getImageIds().stream()).distinct().toList());
        return new MarketplaceListingPageResponse(
                pageResult.getContent().stream().map(item -> response(item, imageMap)).toList(),
                page, size, pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    private MarketplaceListing requireVisible(String listingId) {
        return listings.findByIdAndStatusNot(listingId, MarketplaceListingStatus.UNAVAILABLE)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace listing not found: " + listingId));
    }

    private MarketplaceListing requireOwned(String listingId, String sellerStudentId) {
        MarketplaceListing listing = requireVisible(listingId);
        if (!listing.getSellerStudentId().equals(sellerStudentId.trim())) {
            throw new ResourceNotFoundException("Marketplace listing not found for this seller: " + listingId);
        }
        return listing;
    }

    private MarketplaceListingResponse response(
            MarketplaceListing listing,
            Map<String, MarketplaceImage> imageMap) {
        return new MarketplaceListingResponse(
                listing.getId(), listing.getSellerStudentId(), listing.getSellerName(), listing.getTitle(),
                listing.getDescription(), listing.getCategory(), listing.getPrice(), listing.getCondition(),
                listing.getListingType(), listing.getQuantity(), listing.isNegotiable(), listing.getPickupZone(),
                listing.getStatus(), images.responses(listing.getImageIds(), imageMap), listing.getViewCount(),
                listing.getCreatedAt(), listing.getUpdatedAt());
    }

    private void validateListingValues(
            MarketplaceListingType listingType,
            MarketplaceCondition condition,
            int quantity,
            BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new InvalidRequestException("Listing price must be greater than zero");
        }
        if (listingType == MarketplaceListingType.USED && quantity != 1) {
            throw new InvalidRequestException("Used product quantity must be 1");
        }
        if (listingType == MarketplaceListingType.USED && condition == MarketplaceCondition.NEW) {
            throw new InvalidRequestException("A used listing cannot have NEW condition");
        }
    }

    private void validatePageAndPrices(int page, int size, BigDecimal minimumPrice, BigDecimal maximumPrice) {
        if (page < 0 || size < 1 || size > 50) {
            throw new InvalidRequestException("Page must be at least 0 and size must be between 1 and 50");
        }
        if (minimumPrice != null && minimumPrice.signum() < 0) {
            throw new InvalidRequestException("Minimum price cannot be negative");
        }
        if (maximumPrice != null && maximumPrice.signum() < 0) {
            throw new InvalidRequestException("Maximum price cannot be negative");
        }
        if (minimumPrice != null && maximumPrice != null && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new InvalidRequestException("Minimum price cannot exceed maximum price");
        }
    }

    private Sort sort(MarketplaceSort marketplaceSort) {
        return switch (marketplaceSort == null ? MarketplaceSort.NEWEST : marketplaceSort) {
            case PRICE_LOW_TO_HIGH -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createdAt"));
            case PRICE_HIGH_TO_LOW -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createdAt"));
            case MOST_VIEWED -> Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
            case NEWEST -> Sort.by(Sort.Order.desc("createdAt"));
        };
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
