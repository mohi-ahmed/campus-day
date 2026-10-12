package com.spark.campusday.marketplace.offer.controller;

import com.spark.campusday.marketplace.offer.dto.MarketplaceOfferActionRequest;
import com.spark.campusday.marketplace.offer.dto.MarketplaceOfferRequest;
import com.spark.campusday.marketplace.offer.dto.MarketplaceOfferResponse;
import com.spark.campusday.marketplace.offer.service.MarketplaceOfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceOfferController {
    private final MarketplaceOfferService service;

    @PostMapping("/listings/{listingId}/offers")
    public ResponseEntity<MarketplaceOfferResponse> create(
            @PathVariable @NotBlank String listingId,
            @Valid @RequestBody MarketplaceOfferRequest request) {
        MarketplaceOfferResponse created = service.create(listingId, request);
        return ResponseEntity.created(URI.create("/api/marketplace/offers/" + created.id())).body(created);
    }

    @GetMapping("/offers/received")
    public List<MarketplaceOfferResponse> received(@RequestParam @NotBlank String sellerStudentId) {
        return service.received(sellerStudentId);
    }

    @GetMapping("/offers/sent")
    public List<MarketplaceOfferResponse> sent(@RequestParam @NotBlank String buyerStudentId) {
        return service.sent(buyerStudentId);
    }

    @PatchMapping("/offers/{offerId}/counter")
    public MarketplaceOfferResponse counter(
            @PathVariable @NotBlank String offerId,
            @Valid @RequestBody MarketplaceOfferActionRequest request) {
        return service.counter(offerId, request);
    }

    @PatchMapping("/offers/{offerId}/accept")
    public MarketplaceOfferResponse accept(
            @PathVariable @NotBlank String offerId,
            @Valid @RequestBody MarketplaceOfferActionRequest request) {
        return service.accept(offerId, request);
    }

    @PatchMapping("/offers/{offerId}/reject")
    public MarketplaceOfferResponse reject(
            @PathVariable @NotBlank String offerId,
            @Valid @RequestBody MarketplaceOfferActionRequest request) {
        return service.reject(offerId, request);
    }
}
