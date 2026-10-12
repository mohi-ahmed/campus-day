package com.spark.campusday.autopsy.comment.dto;

import java.util.List;

public record CommentPage(
        List<CommentResponse> comments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
