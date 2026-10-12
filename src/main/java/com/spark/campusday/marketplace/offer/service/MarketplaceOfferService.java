package com.spark.campusday.marketplace.offer.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.marketplace.listing.model.MarketplaceListing;
import com.spark.campusday.marketplace.listing.service.MarketplaceListingService;
import com.spark.campusday.marketplace.offer.dto.MarketplaceOfferActionRequest;
import com.spark.campusday.marketplace.offer.dto.MarketplaceOfferRequest;
import com.spark.campusday.marketplace.offer.dto.MarketplaceOfferResponse;
import com.spark.campusday.marketplace.offer.model.MarketplaceOffer;
import com.spark.campusday.marketplace.offer.model.MarketplaceOfferStatus;
import com.spark.campusday.marketplace.offer.repository.MarketplaceOfferRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketplaceOfferService {
    private static final EnumSet<MarketplaceOfferStatus> ACTIVE_STATUSES = EnumSet.of(
            MarketplaceOfferStatus.PENDING,
            MarketplaceOfferStatus.COUNTERED);

    private final MarketplaceOfferRepository offers;
    private final MarketplaceListingService listings;
    private final StudentService students;

    public MarketplaceOfferResponse create(String listingId, MarketplaceOfferRequest request) {
        MarketplaceListing listing = listings.requireAvailable(listingId);
        Student buyer = students.getByStudentId(request.buyerStudentId().trim());
        if (listing.getSellerStudentId().equals(buyer.getStudentId())) {
            throw new InvalidRequestException("You cannot make an offer on your own listing");
        }
        if (!listing.isNegotiable()) {
            throw new InvalidBusinessStateException("This listing has a fixed price");
        }
        if (offers.existsByListingIdAndBuyerStudentIdAndStatusIn(
                listingId, buyer.getStudentId(), ACTIVE_STATUSES)) {
            throw new ConflictException("You already have an active offer for this listing");
        }
        MarketplaceOffer offer = MarketplaceOffer.builder()
                .listingId(listing.getId())
                .listingTitle(listing.getTitle())
                .buyerStudentId(buyer.getStudentId())
                .buyerName(buyer.getName())
                .sellerStudentId(listing.getSellerStudentId())
                .offeredAmount(request.amount())
                .message(optional(request.message()))
                .build();
        return response(offers.save(offer));
    }

    public List<MarketplaceOfferResponse> received(String sellerStudentId) {
        students.getByStudentId(sellerStudentId);
        return offers.findAllBySellerStudentIdOrderByCreatedAtDesc(sellerStudentId).stream()
                .map(MarketplaceOfferService::response).toList();
    }

    public List<MarketplaceOfferResponse> sent(String buyerStudentId) {
        students.getByStudentId(buyerStudentId);
        return offers.findAllByBuyerStudentIdOrderByCreatedAtDesc(buyerStudentId).stream()
                .map(MarketplaceOfferService::response).toList();
    }

    public MarketplaceOfferResponse counter(String offerId, MarketplaceOfferActionRequest request) {
        MarketplaceOffer offer = requireActive(offerId);
        requireActor(offer.getSellerStudentId(), request.actorStudentId());
        BigDecimal amount = request.counterAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRequestException("A valid counter amount is required");
        }
        MarketplaceOffer saved = offers.save(offer.toBuilder()
                .counterAmount(amount)
                .status(MarketplaceOfferStatus.COUNTERED)
                .build());
        return response(saved);
    }

    public MarketplaceOfferResponse accept(String offerId, MarketplaceOfferActionRequest request) {
        MarketplaceOffer offer = requireActive(offerId);
        String actor = request.actorStudentId().trim();
        boolean sellerAcceptingBuyerOffer = offer.getStatus() == MarketplaceOfferStatus.PENDING
                && offer.getSellerStudentId().equals(actor);
        boolean buyerAcceptingCounter = offer.getStatus() == MarketplaceOfferStatus.COUNTERED
                && offer.getBuyerStudentId().equals(actor);
        if (!sellerAcceptingBuyerOffer && !buyerAcceptingCounter) {
            throw new ResourceNotFoundException("Active offer not found for this student: " + offerId);
        }
        listings.reserve(offer.getListingId(), offer.getSellerStudentId());
        MarketplaceOffer accepted = offers.save(offer.toBuilder()
                .status(MarketplaceOfferStatus.ACCEPTED)
                .build());
        List<MarketplaceOffer> otherOffers = offers.findAllByListingIdAndStatusIn(
                offer.getListingId(), ACTIVE_STATUSES).stream()
                .filter(item -> !item.getId().equals(offerId))
                .map(item -> item.toBuilder().status(MarketplaceOfferStatus.REJECTED).build())
                .toList();
        offers.saveAll(otherOffers);
        return response(accepted);
    }

    public MarketplaceOfferResponse reject(String offerId, MarketplaceOfferActionRequest request) {
        MarketplaceOffer offer = requireActive(offerId);
        String actor = request.actorStudentId().trim();
        if (!offer.getSellerStudentId().equals(actor) && !offer.getBuyerStudentId().equals(actor)) {
            throw new ResourceNotFoundException("Active offer not found for this student: " + offerId);
        }
        MarketplaceOfferStatus status = offer.getBuyerStudentId().equals(actor)
                ? MarketplaceOfferStatus.CANCELLED
                : MarketplaceOfferStatus.REJECTED;
        return response(offers.save(offer.toBuilder().status(status).build()));
    }

    private MarketplaceOffer requireActive(String offerId) {
        return offers.findById(offerId)
                .filter(offer -> ACTIVE_STATUSES.contains(offer.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Active offer not found: " + offerId));
    }

    private void requireActor(String expectedStudentId, String actualStudentId) {
        if (!expectedStudentId.equals(actualStudentId.trim())) {
            throw new ResourceNotFoundException("Offer not found for this student");
        }
    }

    private static MarketplaceOfferResponse response(MarketplaceOffer offer) {
        return new MarketplaceOfferResponse(
                offer.getId(), offer.getListingId(), offer.getListingTitle(), offer.getBuyerStudentId(),
                offer.getBuyerName(), offer.getSellerStudentId(), offer.getOfferedAmount(),
                offer.getCounterAmount(), offer.getMessage(), offer.getStatus(),
                offer.getCreatedAt(), offer.getUpdatedAt());
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
