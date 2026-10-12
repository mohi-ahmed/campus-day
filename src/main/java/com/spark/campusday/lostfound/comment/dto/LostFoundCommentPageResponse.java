package com.spark.campusday.lostfound.comment.dto;

import java.util.List;

public record LostFoundCommentPageResponse(
        List<LostFoundCommentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
