package com.spark.campusday.marketplace.offer.dto;

import com.spark.campusday.marketplace.offer.model.MarketplaceOfferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketplaceOfferResponse(
        String id,
        String listingId,
        String listingTitle,
        String buyerStudentId,
        String buyerName,
        String sellerStudentId,
        BigDecimal offeredAmount,
        BigDecimal counterAmount,
        String message,
        MarketplaceOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
