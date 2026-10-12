package com.spark.campusday.marketplace.message.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.marketplace.listing.dto.MarketplaceListingResponse;
import com.spark.campusday.marketplace.listing.service.MarketplaceListingService;
import com.spark.campusday.marketplace.message.dto.MarketplaceMessageRequest;
import com.spark.campusday.marketplace.message.dto.MarketplaceMessageResponse;
import com.spark.campusday.marketplace.message.model.MarketplaceMessage;
import com.spark.campusday.marketplace.message.repository.MarketplaceMessageRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarketplaceMessageService {
    private final MarketplaceMessageRepository messages;
    private final MarketplaceListingService listings;
    private final StudentService students;

    public MarketplaceMessageResponse send(MarketplaceMessageRequest request) {
        Student sender = students.getByStudentId(request.senderStudentId().trim());
        Student recipient = students.getByStudentId(request.recipientStudentId().trim());
        if (sender.getStudentId().equals(recipient.getStudentId())) {
            throw new InvalidRequestException("You cannot message yourself");
        }

        String listingId = optional(request.listingId());
        String listingTitle = null;
        if (listingId != null) {
            MarketplaceListingResponse listing = listings.get(listingId);
            if (!listing.sellerStudentId().equals(recipient.getStudentId())) {
                throw new InvalidRequestException("The message recipient is not the listing seller");
            }
            listingTitle = listing.title();
        }

        MarketplaceMessage message = MarketplaceMessage.builder()
                .senderStudentId(sender.getStudentId())
                .senderName(sender.getName())
                .recipientStudentId(recipient.getStudentId())
                .recipientName(recipient.getName())
                .listingId(listingId)
                .listingTitle(listingTitle)
                .body(request.body().trim())
                .build();
        return response(messages.save(message));
    }

    public List<MarketplaceMessageResponse> conversation(
            String studentId,
            String otherStudentId,
            String listingId) {
        String student = studentId.trim();
        String otherStudent = otherStudentId.trim();
        students.getByStudentId(student);
        students.getByStudentId(otherStudent);
        String normalizedListingId = optional(listingId);
        return messages.findAllBySenderStudentIdInAndRecipientStudentIdInOrderByCreatedAtAsc(
                        Set.of(student, otherStudent), Set.of(student, otherStudent)).stream()
                .filter(message -> normalizedListingId == null
                        || normalizedListingId.equals(message.getListingId()))
                .map(MarketplaceMessageService::response)
                .toList();
    }

    private static MarketplaceMessageResponse response(MarketplaceMessage message) {
        return new MarketplaceMessageResponse(
                message.getId(), message.getSenderStudentId(), message.getSenderName(),
                message.getRecipientStudentId(), message.getRecipientName(), message.getListingId(),
                message.getListingTitle(), message.getBody(), message.getCreatedAt());
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
