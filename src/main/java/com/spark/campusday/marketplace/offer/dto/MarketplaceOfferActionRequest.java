package com.spark.campusday.marketplace.offer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MarketplaceOfferActionRequest(
        @NotBlank @Size(max = 64) String actorStudentId,
        @DecimalMin("1.00") BigDecimal counterAmount
) {
}
