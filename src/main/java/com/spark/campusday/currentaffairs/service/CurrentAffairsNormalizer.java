package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.currentaffairs.config.CurrentAffairsProperties;
import com.spark.campusday.currentaffairs.dto.NewsDataResponse;
import com.spark.campusday.currentaffairs.model.CurrentAffairsArticle;
import com.spark.campusday.currentaffairs.model.CurrentAffairsCategory;
import com.spark.campusday.currentaffairs.model.CurrentAffairsContentType;
import com.spark.campusday.currentaffairs.model.CurrentAffairsLanguage;
import com.spark.campusday.currentaffairs.model.CurrentAffairsProvider;
import com.spark.campusday.currentaffairs.model.CurrentAffairsRegion;
import com.spark.campusday.currentaffairs.model.CurrentAffairsRightsStatus;
import com.spark.campusday.currentaffairs.model.CurrentAffairsVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentAffairsNormalizer {
    private static final DateTimeFormatter NEWSDATA_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> BANGLADESH_TERMS = List.of(
            "bangladesh", "bangladeshi", "dhaka", "chattogram", "chittagong",
            "বাংলাদেশ", "ঢাকা", "চট্টগ্রাম");

    private final CurrentAffairsProperties properties;
    private final CurrentAffairsMediaService mediaService;

    public Optional<CurrentAffairsArticle> normalize(
            NewsDataResponse.Article source,
            String externalArticleId,
            String importBatchId,
            CurrentAffairsArticle existing) {
        if (Boolean.TRUE.equals(source.duplicate())) return Optional.empty();

        String title = clean(source.title(), 400);
        String articleUrl = safeHttpUrl(source.link());
        String sourceName = clean(source.sourceName(), 160);
        Instant publishedAt = parseTime(source.publishedAt(), source.publishedTimeZone());
        if (title == null || articleUrl == null || sourceName == null || publishedAt == null) {
            return Optional.empty();
        }

        String description = clean(source.description(), 1200);
        if (description == null) description = title;
        CurrentAffairsRegion region = region(title + " " + description);
        CurrentAffairsCategory category = category(source.category(), region);
        CurrentAffairsRightsStatus rightsStatus = properties.sourceRightsCleared()
                ? CurrentAffairsRightsStatus.CLEARED
                : CurrentAffairsRightsStatus.REVIEW_REQUIRED;
        CurrentAffairsMediaService.MediaReference media = mediaService.resolve(
                safeHttpUrl(source.imageUrl()),
                externalArticleId,
                rightsStatus,
                existing == null ? null : existing.getCloudinaryImageUrl(),
                existing == null ? null : existing.getCloudinaryPublicId());

        CurrentAffairsArticle.CurrentAffairsArticleBuilder builder = existing == null
                ? CurrentAffairsArticle.builder()
                : existing.toBuilder();
        return Optional.of(builder
                .provider(CurrentAffairsProvider.NEWSDATA_IO)
                .externalArticleId(externalArticleId)
                .title(title)
                .description(description)
                .body(List.of(description))
                .keywords(cleanList(source.keywords(), 20, 80))
                .category(category)
                .language(language(source.language()))
                .region(region)
                .contentType(contentType(category))
                .sourceId(clean(source.sourceId(), 160))
                .sourceName(sourceName)
                .publisherUrl(safeHttpUrl(source.sourceUrl()))
                .sourceUrl(articleUrl)
                .author(joinCreators(source.creator()))
                .publishedAt(publishedAt)
                .originalImageUrl(media.originalUrl())
                .cloudinaryImageUrl(media.cloudinaryUrl())
                .cloudinaryPublicId(media.cloudinaryPublicId())
                .readTimeMinutes(readTime(description))
                .featured(source.category() != null && source.category().stream()
                        .anyMatch(value -> "top".equalsIgnoreCase(value)))
                .relatedGroup(category.value().toLowerCase(Locale.ROOT))
                .rightsStatus(rightsStatus)
                .verificationStatus(CurrentAffairsVerificationStatus.UNVERIFIED)
                .trainingEligible(false)
                .predictionEligible(false)
                .importBatchId(importBatchId)
                .schemaVersion(properties.schemaVersion())
                .providerFetchedAt(parseTime(source.fetchedAt(), "UTC"))
                .lastSyncedAt(Instant.now())
                .build());
    }

    private static CurrentAffairsLanguage language(String value) {
        if (value != null && (value.equalsIgnoreCase("bengali")
                || value.equalsIgnoreCase("bangla")
                || value.equalsIgnoreCase("bn"))) {
            return CurrentAffairsLanguage.BENGALI;
        }
        return CurrentAffairsLanguage.ENGLISH;
    }

    private static CurrentAffairsRegion region(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return BANGLADESH_TERMS.stream().anyMatch(normalized::contains)
                ? CurrentAffairsRegion.BANGLADESH
                : CurrentAffairsRegion.INTERNATIONAL;
    }

    private static CurrentAffairsCategory category(
            List<String> providerCategories,
            CurrentAffairsRegion region) {
        List<String> values = providerCategories == null
                ? List.of()
                : providerCategories.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
        if (values.contains("education")) return CurrentAffairsCategory.EDUCATION;
        if (values.contains("science")) return CurrentAffairsCategory.SCIENCE;
        if (values.contains("technology")) return CurrentAffairsCategory.TECHNOLOGY;
        if (values.contains("health")) return CurrentAffairsCategory.HEALTH;
        if (values.contains("business")) return CurrentAffairsCategory.BUSINESS;
        if (values.contains("sports")) return CurrentAffairsCategory.SPORTS;
        if (values.contains("politics")) return CurrentAffairsCategory.POLITICS;
        if (values.contains("environment")) return CurrentAffairsCategory.ENVIRONMENT;
        return region == CurrentAffairsRegion.BANGLADESH
                ? CurrentAffairsCategory.BANGLADESH
                : CurrentAffairsCategory.INTERNATIONAL;
    }

    private static CurrentAffairsContentType contentType(CurrentAffairsCategory category) {
        return switch (category) {
            case EDUCATION, SCIENCE, TECHNOLOGY, HEALTH, BUSINESS, POLITICS, ENVIRONMENT ->
                    CurrentAffairsContentType.CURRENT_AFFAIRS;
            case BANGLADESH, INTERNATIONAL, SPORTS -> CurrentAffairsContentType.NEWS;
        };
    }

    private static int readTime(String description) {
        int words = description.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    private static String joinCreators(List<String> creators) {
        List<String> cleaned = cleanList(creators, 3, 100);
        return cleaned.isEmpty() ? null : String.join(", ", cleaned);
    }

    private static List<String> cleanList(List<String> values, int limit, int maxLength) {
        if (values == null) return List.of();
        return values.stream()
                .map(value -> clean(value, maxLength))
                .filter(value -> value != null)
                .distinct()
                .limit(limit)
                .toList();
    }

    private static String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String cleaned = HtmlUtils.htmlUnescape(value)
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) return null;
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim();
    }

    private static String safeHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    ? uri.toString()
                    : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Instant parseTime(String value, String timeZone) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                ZoneId zone = timeZone == null || timeZone.isBlank()
                        ? ZoneOffset.UTC
                        : ZoneId.of(timeZone);
                return LocalDateTime.parse(value, NEWSDATA_TIME).atZone(zone).toInstant();
            } catch (DateTimeParseException | java.time.zone.ZoneRulesException exception) {
                return null;
            }
        }
    }
}
