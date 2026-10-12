package com.spark.campusday.currentaffairs.dto;

import java.util.List;

public record CurrentAffairsPageResponse(
        List<CurrentAffairsArticleResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String nextCursor
) {
}
