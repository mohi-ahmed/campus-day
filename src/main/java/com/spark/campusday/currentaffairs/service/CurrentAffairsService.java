package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.currentaffairs.dto.CurrentAffairsArticleResponse;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsFilter;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsPageResponse;
import com.spark.campusday.currentaffairs.model.CurrentAffairsArticle;
import com.spark.campusday.currentaffairs.model.CurrentAffairsUserState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrentAffairsService {
    private final CurrentAffairsQueryService queryService;
    private final CurrentAffairsUserStateService userStates;

    public CurrentAffairsPageResponse list(
            String studentId,
            String query,
            String category,
            String region,
            String language,
            String source,
            String contentType,
            String readStatus,
            Boolean bookmarked,
            int page,
            int size) {
        String normalizedStudentId = userStates.optionalStudent(studentId);
        CurrentAffairsFilter filter = CurrentAffairsFilter.from(
                query, category, region, language, source, contentType);
        CurrentAffairsUserStateService.ArticleSelection selection =
                userStates.selection(normalizedStudentId, readStatus, bookmarked);

        Page<CurrentAffairsArticle> result;
        if (selection.includedArticleIds() != null && selection.includedArticleIds().isEmpty()) {
            result = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        } else if (selection.personalized()) {
            result = queryService.searchPersonalized(
                    filter,
                    selection.includedArticleIds(),
                    selection.excludedArticleIds(),
                    page,
                    size);
        } else {
            result = queryService.searchShared(filter, page, size);
        }
        return page(result, normalizedStudentId);
    }

    public List<CurrentAffairsArticleResponse> highlights(String studentId) {
        String normalizedStudentId = userStates.optionalStudent(studentId);
        List<CurrentAffairsArticle> articles = queryService.highlights();
        Map<String, CurrentAffairsUserState> stateMap = stateMap(normalizedStudentId, articles);
        return articles.stream().map(article -> response(article, stateMap.get(article.getId()))).toList();
    }

    public CurrentAffairsArticleResponse get(String studentId, String articleId) {
        String normalizedStudentId = userStates.optionalStudent(studentId);
        CurrentAffairsArticle article = queryService.requireArticle(articleId);
        CurrentAffairsUserState state = userStates.findForArticles(
                normalizedStudentId, List.of(articleId)).get(articleId);
        return response(article, state);
    }

    public void bookmark(String studentId, String articleId, boolean bookmarked) {
        userStates.setBookmarked(studentId, articleId, bookmarked);
    }

    public void markRead(String studentId, String articleId, boolean read) {
        userStates.setRead(studentId, articleId, read);
    }

    private CurrentAffairsPageResponse page(
            Page<CurrentAffairsArticle> page,
            String studentId) {
        Map<String, CurrentAffairsUserState> stateMap = stateMap(studentId, page.getContent());
        List<CurrentAffairsArticleResponse> items = page.getContent().stream()
                .map(article -> response(article, stateMap.get(article.getId())))
                .toList();
        return new CurrentAffairsPageResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext() ? Integer.toString(page.getNumber() + 1) : null);
    }

    private Map<String, CurrentAffairsUserState> stateMap(
            String studentId,
            List<CurrentAffairsArticle> articles) {
        return userStates.findForArticles(
                studentId,
                articles.stream().map(CurrentAffairsArticle::getId).toList());
    }

    private static CurrentAffairsArticleResponse response(
            CurrentAffairsArticle article,
            CurrentAffairsUserState state) {
        boolean bookmarked = state != null && state.isBookmarked();
        boolean read = state != null && state.isRead();
        String imageUrl = article.getCloudinaryImageUrl() == null
                ? article.getOriginalImageUrl()
                : article.getCloudinaryImageUrl();
        return new CurrentAffairsArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getDescription(),
                article.getBody(),
                article.getCategory(),
                article.getLanguage(),
                article.getRegion(),
                article.getContentType(),
                article.getSourceName(),
                article.getSourceUrl(),
                article.getAuthor(),
                article.getPublishedAt(),
                imageUrl,
                article.getTitle(),
                article.getReadTimeMinutes(),
                article.isFeatured(),
                article.getRelatedGroup(),
                bookmarked,
                read,
                read ? state.getReadAt() : null);
    }
}
