package com.spark.campusday.currentaffairs.dto;

import com.spark.campusday.currentaffairs.model.CurrentAffairsCategory;
import com.spark.campusday.currentaffairs.model.CurrentAffairsContentType;
import com.spark.campusday.currentaffairs.model.CurrentAffairsLanguage;
import com.spark.campusday.currentaffairs.model.CurrentAffairsRegion;

import java.time.Instant;
import java.util.List;

public record CurrentAffairsArticleResponse(
        String id,
        String title,
        String description,
        List<String> body,
        CurrentAffairsCategory category,
        CurrentAffairsLanguage language,
        CurrentAffairsRegion region,
        CurrentAffairsContentType contentType,
        String sourceName,
        String sourceUrl,
        String author,
        Instant publishedAt,
        String imageUrl,
        String imageAlt,
        int readTimeMinutes,
        boolean featured,
        String relatedGroup,
        boolean isBookmarked,
        boolean isRead,
        Instant readAt
) {
}
