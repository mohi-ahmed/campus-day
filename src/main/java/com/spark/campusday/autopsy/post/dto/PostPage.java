package com.spark.campusday.autopsy.post.dto;

import java.util.List;

public record PostPage(
        List<PostResponse> posts,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
