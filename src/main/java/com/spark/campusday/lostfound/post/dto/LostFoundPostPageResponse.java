package com.spark.campusday.lostfound.post.dto;

import java.util.List;

public record LostFoundPostPageResponse(
        List<LostFoundPostResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
