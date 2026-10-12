package com.spark.campusday.currentaffairs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsDataResponse(
        String status,
        Integer totalResults,
        List<Article> results,
        String nextPage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Article(
            @JsonProperty("article_id") String articleId,
            String link,
            String title,
            String description,
            String content,
            List<String> keywords,
            List<String> creator,
            String language,
            List<String> country,
            List<String> category,
            String datatype,
            @JsonProperty("pubDate") String publishedAt,
            @JsonProperty("pubDateTZ") String publishedTimeZone,
            @JsonProperty("fetched_at") String fetchedAt,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("source_id") String sourceId,
            @JsonProperty("source_name") String sourceName,
            @JsonProperty("source_url") String sourceUrl,
            @JsonProperty("source_priority") Integer sourcePriority,
            Boolean duplicate
    ) {
    }
}
