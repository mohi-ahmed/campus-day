package com.spark.campusday.currentaffairs.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.spark.campusday.currentaffairs.config.CurrentAffairsProperties;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsFilter;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsImportResult;
import com.spark.campusday.currentaffairs.dto.NewsDataResponse;
import com.spark.campusday.currentaffairs.model.CurrentAffairsArticle;
import com.spark.campusday.currentaffairs.model.CurrentAffairsImportBatch;
import com.spark.campusday.currentaffairs.model.CurrentAffairsImportStatus;
import com.spark.campusday.currentaffairs.model.CurrentAffairsProvider;
import com.spark.campusday.currentaffairs.model.CurrentAffairsRightsStatus;
import com.spark.campusday.currentaffairs.model.CurrentAffairsSourceRecord;
import com.spark.campusday.currentaffairs.model.CurrentAffairsVerificationStatus;
import com.spark.campusday.currentaffairs.repository.CurrentAffairsArticleRepository;
import com.spark.campusday.currentaffairs.repository.CurrentAffairsImportBatchRepository;
import com.spark.campusday.currentaffairs.repository.CurrentAffairsSourceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentAffairsImportService {
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final NewsDataClient newsDataClient;
    private final CurrentAffairsNormalizer normalizer;
    private final CurrentAffairsArticleRepository articles;
    private final CurrentAffairsSourceRecordRepository sourceRecords;
    private final CurrentAffairsImportBatchRepository batches;
    private final CurrentAffairsCacheService cacheService;
    private final CurrentAffairsQueryService queryService;
    private final CurrentAffairsProperties properties;
    private final ObjectMapper objectMapper;

    public CurrentAffairsImportResult importLatest() {
        String batchId = UUID.randomUUID().toString();
        CurrentAffairsImportBatch batch = batches.save(CurrentAffairsImportBatch.builder()
                .id(batchId)
                .provider(CurrentAffairsProvider.NEWSDATA_IO)
                .schemaVersion(properties.schemaVersion())
                .startedAt(Instant.now())
                .status(CurrentAffairsImportStatus.RUNNING)
                .build());
        try {
            NewsDataResponse response = newsDataClient.fetchLatest();
            List<NewsDataResponse.Article> fetched = response.results() == null
                    ? List.of()
                    : response.results();
            LinkedHashMap<String, NewsDataResponse.Article> unique = deduplicate(fetched);
            storeEvidence(batchId, unique);

            Map<String, CurrentAffairsArticle> existing = articles
                    .findAllByProviderAndExternalArticleIdIn(
                            CurrentAffairsProvider.NEWSDATA_IO,
                            unique.keySet())
                    .stream()
                    .collect(Collectors.toMap(
                            CurrentAffairsArticle::getExternalArticleId,
                            Function.identity()));

            List<CurrentAffairsArticle> normalized = new ArrayList<>();
            unique.forEach((externalId, source) -> normalizer
                    .normalize(source, externalId, batchId, existing.get(externalId))
                    .ifPresent(normalized::add));
            if (!normalized.isEmpty()) {
                articles.saveAll(normalized);
                cacheService.evictContentCaches();
                warmContentCaches();
            }

            int rejected = fetched.size() - normalized.size();
            batches.save(batch.toBuilder()
                    .completedAt(Instant.now())
                    .status(CurrentAffairsImportStatus.COMPLETED)
                    .fetchedCount(fetched.size())
                    .persistedCount(normalized.size())
                    .rejectedCount(Math.max(0, rejected))
                    .build());
            return new CurrentAffairsImportResult(
                    batchId, fetched.size(), normalized.size(), Math.max(0, rejected));
        } catch (RuntimeException exception) {
            batches.save(batch.toBuilder()
                    .completedAt(Instant.now())
                    .status(CurrentAffairsImportStatus.FAILED)
                    .error(safeError(exception.getMessage()))
                    .build());
            throw exception;
        }
    }

    private LinkedHashMap<String, NewsDataResponse.Article> deduplicate(
            List<NewsDataResponse.Article> fetched) {
        LinkedHashMap<String, NewsDataResponse.Article> unique = new LinkedHashMap<>();
        fetched.forEach(article -> unique.putIfAbsent(externalId(article), article));
        return unique;
    }

    private void storeEvidence(
            String batchId,
            LinkedHashMap<String, NewsDataResponse.Article> unique) {
        Set<String> existingEvidence = sourceRecords
                .findAllByProviderAndExternalArticleIdIn(
                        CurrentAffairsProvider.NEWSDATA_IO,
                        unique.keySet())
                .stream()
                .map(record -> evidenceKey(record.getExternalArticleId(), record.getRawChecksum()))
                .collect(Collectors.toSet());

        List<CurrentAffairsSourceRecord> newRecords = new ArrayList<>();
        int ordinal = 0;
        for (Map.Entry<String, NewsDataResponse.Article> entry : unique.entrySet()) {
            Map<String, Object> payload = objectMapper.convertValue(entry.getValue(), PAYLOAD_TYPE);
            String checksum = checksum(payload);
            if (!existingEvidence.add(evidenceKey(entry.getKey(), checksum))) {
                ordinal++;
                continue;
            }
            newRecords.add(CurrentAffairsSourceRecord.builder()
                    .provider(CurrentAffairsProvider.NEWSDATA_IO)
                    .externalArticleId(entry.getKey())
                    .importBatchId(batchId)
                    .ordinal(ordinal++)
                    .schemaVersion(properties.schemaVersion())
                    .rawChecksum(checksum)
                    .payload(payload)
                    .rightsStatus(properties.sourceRightsCleared()
                            ? CurrentAffairsRightsStatus.CLEARED
                            : CurrentAffairsRightsStatus.REVIEW_REQUIRED)
                    .verificationStatus(CurrentAffairsVerificationStatus.UNVERIFIED)
                    .build());
        }
        if (!newRecords.isEmpty()) sourceRecords.saveAll(newRecords);
    }

    private void warmContentCaches() {
        queryService.highlights();
        queryService.searchShared(
                new CurrentAffairsFilter(null, null, null, null, null, null),
                0,
                20);
    }

    private String checksum(Map<String, Object> payload) {
        try {
            return CurrentAffairsDigest.sha256(objectMapper.writeValueAsString(payload));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not preserve NewsData.io source evidence", exception);
        }
    }

    private static String externalId(NewsDataResponse.Article article) {
        if (article.articleId() != null && !article.articleId().isBlank()) {
            return article.articleId().trim();
        }
        String fallback = String.valueOf(article.link()) + "|"
                + String.valueOf(article.title()) + "|"
                + String.valueOf(article.publishedAt());
        return "derived-" + CurrentAffairsDigest.sha256(fallback);
    }

    private static String evidenceKey(String externalId, String checksum) {
        return externalId + ':' + checksum;
    }

    private static String safeError(String message) {
        if (message == null || message.isBlank()) return "Current Affairs import failed";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
