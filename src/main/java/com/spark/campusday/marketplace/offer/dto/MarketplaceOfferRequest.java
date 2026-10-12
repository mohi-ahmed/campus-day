package com.spark.campusday.marketplace.offer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MarketplaceOfferRequest(
        @NotBlank @Size(max = 64) String buyerStudentId,
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        @Size(max = 1000) String message
) {
}
