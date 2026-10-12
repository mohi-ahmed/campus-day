package com.spark.campusday.currentaffairs.dto;

import com.spark.campusday.currentaffairs.model.CurrentAffairsCategory;
import com.spark.campusday.currentaffairs.model.CurrentAffairsContentType;
import com.spark.campusday.currentaffairs.model.CurrentAffairsLanguage;
import com.spark.campusday.currentaffairs.model.CurrentAffairsRegion;

public record CurrentAffairsFilter(
        String query,
        CurrentAffairsCategory category,
        CurrentAffairsRegion region,
        CurrentAffairsLanguage language,
        String source,
        CurrentAffairsContentType contentType
) {
    public static CurrentAffairsFilter from(
            String query,
            String category,
            String region,
            String language,
            String source,
            String contentType) {
        return new CurrentAffairsFilter(
                normalize(query),
                CurrentAffairsCategory.parse(category),
                CurrentAffairsRegion.parse(region),
                CurrentAffairsLanguage.parse(language),
                normalize(source),
                CurrentAffairsContentType.parse(contentType));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
