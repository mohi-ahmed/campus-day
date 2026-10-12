package com.spark.campusday.marketplace.wanted.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WantedResponseRequest(
        @NotBlank @Size(max = 64) String sellerStudentId,
        @Size(max = 64) String listingId,
        @NotNull @DecimalMin("1.00") BigDecimal price,
        @NotBlank @Size(max = 1000) String message
) {
}
