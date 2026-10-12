package com.spark.campusday.currentaffairs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "current_affairs_articles")
@CompoundIndexes({
        @CompoundIndex(name = "current_affairs_provider_external_unique",
                def = "{'provider': 1, 'externalArticleId': 1}", unique = true),
        @CompoundIndex(name = "current_affairs_type_published",
                def = "{'contentType': 1, 'publishedAt': -1}"),
        @CompoundIndex(name = "current_affairs_category_published",
                def = "{'category': 1, 'publishedAt': -1}"),
        @CompoundIndex(name = "current_affairs_region_language_published",
                def = "{'region': 1, 'language': 1, 'publishedAt': -1}"),
        @CompoundIndex(name = "current_affairs_source_published",
                def = "{'sourceName': 1, 'publishedAt': -1}")
})
public class CurrentAffairsArticle {
    @Id
    private String id;

    private CurrentAffairsProvider provider;
    private String externalArticleId;
    private String title;
    private String description;
    private List<String> body;
    private List<String> keywords;
    private CurrentAffairsCategory category;
    private CurrentAffairsLanguage language;
    private CurrentAffairsRegion region;
    private CurrentAffairsContentType contentType;
    private String sourceId;
    private String sourceName;
    private String publisherUrl;
    private String sourceUrl;
    private String author;

    @Indexed(name = "current_affairs_published")
    private Instant publishedAt;

    private String originalImageUrl;
    private String cloudinaryImageUrl;
    private String cloudinaryPublicId;
    private int readTimeMinutes;
    private boolean featured;
    private String relatedGroup;
    private CurrentAffairsRightsStatus rightsStatus;
    private CurrentAffairsVerificationStatus verificationStatus;
    private boolean trainingEligible;
    private boolean predictionEligible;
    private String importBatchId;
    private String schemaVersion;
    private Instant providerFetchedAt;
    private Instant lastSyncedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
