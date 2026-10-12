package com.spark.campusday.marketplace.wanted.dto;

import java.util.List;

public record WantedPostPageResponse(
        List<WantedPostDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
