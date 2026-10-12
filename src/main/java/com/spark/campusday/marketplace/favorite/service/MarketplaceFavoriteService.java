package com.spark.campusday.marketplace.favorite.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.marketplace.config.MarketplaceCacheNames;
import com.spark.campusday.marketplace.favorite.dto.MarketplaceFavoriteResponse;
import com.spark.campusday.marketplace.favorite.model.MarketplaceFavorite;
import com.spark.campusday.marketplace.favorite.repository.MarketplaceFavoriteRepository;
import com.spark.campusday.marketplace.listing.service.MarketplaceListingService;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketplaceFavoriteService {
    private final MarketplaceFavoriteRepository favorites;
    private final MarketplaceListingService listings;
    private final StudentService students;

    @Cacheable(cacheNames = MarketplaceCacheNames.FAVORITES, key = "#studentId", sync = true)
    public MarketplaceFavoriteResponse list(String studentId) {
        students.getByStudentId(studentId);
        List<String> listingIds = favorites.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(MarketplaceFavorite::getListingId)
                .toList();
        return new MarketplaceFavoriteResponse(studentId, listings.findVisibleByIds(listingIds));
    }

    @CacheEvict(cacheNames = MarketplaceCacheNames.FAVORITES, key = "#studentId")
    public void save(String listingId, String studentId) {
        students.getByStudentId(studentId);
        var listing = listings.requireAvailable(listingId);
        if (listing.getSellerStudentId().equals(studentId)) {
            throw new ConflictException("You cannot save your own listing");
        }
        try {
            favorites.save(MarketplaceFavorite.builder()
                    .studentId(studentId)
                    .listingId(listingId)
                    .build());
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Listing is already saved", exception);
        }
    }

    @CacheEvict(cacheNames = MarketplaceCacheNames.FAVORITES, key = "#studentId")
    public void remove(String listingId, String studentId) {
        MarketplaceFavorite favorite = favorites.findByStudentIdAndListingId(studentId, listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved listing not found: " + listingId));
        favorites.delete(favorite);
    }
}
