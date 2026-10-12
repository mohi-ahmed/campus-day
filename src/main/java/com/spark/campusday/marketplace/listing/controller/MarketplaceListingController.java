package com.spark.campusday.marketplace.listing.controller;

import com.spark.campusday.marketplace.listing.dto.MarketplaceListingCreateRequest;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingPageResponse;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingResponse;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingStatusRequest;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingUpdateRequest;
import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplaceListingType;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
import com.spark.campusday.marketplace.listing.model.MarketplaceSort;
import com.spark.campusday.marketplace.listing.service.MarketplaceListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/marketplace/listings")
@RequiredArgsConstructor
public class MarketplaceListingController {
    private final MarketplaceListingService service;

    @PostMapping
    public ResponseEntity<MarketplaceListingResponse> create(
            @Valid @RequestBody MarketplaceListingCreateRequest request) {
        MarketplaceListingResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/marketplace/listings/" + created.id())).body(created);
    }

    @GetMapping
    public MarketplaceListingPageResponse list(
            @RequestParam(required = false) MarketplaceCategory category,
            @RequestParam(required = false) MarketplaceCondition condition,
            @RequestParam(required = false) MarketplaceListingType listingType,
            @RequestParam(required = false) MarketplacePickupZone pickupZone,
            @RequestParam(required = false) Boolean negotiable,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minimumPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maximumPrice,
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(defaultValue = "NEWEST") MarketplaceSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.list(category, condition, listingType, pickupZone, negotiable,
                minimumPrice, maximumPrice, query, sort, page, size);
    }

    @GetMapping("/{listingId}")
    public MarketplaceListingResponse byId(@PathVariable @NotBlank String listingId) {
        return service.get(listingId);
    }

    @GetMapping("/seller/{studentId}")
    public MarketplaceListingPageResponse sellerListings(
            @PathVariable @NotBlank String studentId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.listSellerListings(studentId, page, size);
    }

    @PutMapping("/{listingId}")
    public MarketplaceListingResponse update(
            @PathVariable @NotBlank String listingId,
            @Valid @RequestBody MarketplaceListingUpdateRequest request) {
        return service.update(listingId, request);
    }

    @PatchMapping("/{listingId}/status")
    public MarketplaceListingResponse changeStatus(
            @PathVariable @NotBlank String listingId,
            @Valid @RequestBody MarketplaceListingStatusRequest request) {
        return service.changeStatus(listingId, request);
    }
}
