package com.spark.campusday.careerprep.question.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore) {
}
