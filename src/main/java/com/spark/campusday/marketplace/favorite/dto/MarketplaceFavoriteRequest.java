package com.spark.campusday.marketplace.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketplaceFavoriteRequest(
        @NotBlank @Size(max = 64) String studentId
) {
}
