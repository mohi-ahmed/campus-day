package com.spark.campusday.marketplace.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketplaceMessageRequest(
        @NotBlank @Size(max = 64) String senderStudentId,
        @NotBlank @Size(max = 64) String recipientStudentId,
        @Size(max = 64) String listingId,
        @NotBlank @Size(max = 2000) String body
) {
}
