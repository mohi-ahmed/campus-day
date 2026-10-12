package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.currentaffairs.model.CurrentAffairsUserState;
import com.spark.campusday.currentaffairs.repository.CurrentAffairsUserStateRepository;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentAffairsUserStateService {
    private final CurrentAffairsUserStateRepository states;
    private final CurrentAffairsQueryService articles;
    private final StudentService students;

    public String optionalStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) return null;
        String normalized = studentId.trim();
        students.getByStudentId(normalized);
        return normalized;
    }

    public String requireStudent(String studentId) {
        String normalized = optionalStudent(studentId);
        if (normalized == null) {
            throw new InvalidRequestException("studentId is required for personal current affairs data");
        }
        return normalized;
    }

    public Map<String, CurrentAffairsUserState> findForArticles(
            String studentId,
            Collection<String> articleIds) {
        if (studentId == null || articleIds.isEmpty()) return Map.of();
        return states.findAllByStudentIdAndArticleIdIn(studentId, articleIds).stream()
                .collect(Collectors.toMap(CurrentAffairsUserState::getArticleId, Function.identity()));
    }

    public ArticleSelection selection(
            String studentId,
            String readStatus,
            Boolean bookmarked) {
        boolean hasReadFilter = readStatus != null && !readStatus.isBlank();
        if (!hasReadFilter && bookmarked == null) return ArticleSelection.shared();
        if (studentId == null) {
            throw new InvalidRequestException("studentId is required for personal current affairs data");
        }
        String normalizedStudentId = studentId;

        Set<String> included = null;
        Set<String> excluded = new HashSet<>();
        if (bookmarked != null) {
            Set<String> bookmarkedIds = states.findAllByStudentIdAndBookmarkedTrue(normalizedStudentId).stream()
                    .map(CurrentAffairsUserState::getArticleId)
                    .collect(Collectors.toSet());
            if (bookmarked) included = bookmarkedIds;
            else excluded.addAll(bookmarkedIds);
        }
        if (hasReadFilter) {
            Set<String> readIds = states.findAllByStudentIdAndReadTrue(normalizedStudentId).stream()
                    .map(CurrentAffairsUserState::getArticleId)
                    .collect(Collectors.toSet());
            if ("read".equalsIgnoreCase(readStatus.trim())) {
                included = intersect(included, readIds);
            } else if ("unread".equalsIgnoreCase(readStatus.trim())) {
                excluded.addAll(readIds);
            } else {
                throw new InvalidRequestException("readStatus must be read or unread");
            }
        }
        return new ArticleSelection(included, Set.copyOf(excluded), true);
    }

    public void setBookmarked(String studentId, String articleId, boolean bookmarked) {
        String normalizedStudentId = requireStudent(studentId);
        articles.requireArticle(articleId);
        Instant now = Instant.now();
        CurrentAffairsUserState current = states.findByStudentIdAndArticleId(normalizedStudentId, articleId)
                .orElseGet(() -> CurrentAffairsUserState.builder()
                        .studentId(normalizedStudentId)
                        .articleId(articleId)
                        .build());
        persistOrDelete(current.toBuilder()
                .bookmarked(bookmarked)
                .bookmarkedAt(bookmarked ? now : null)
                .build());
    }

    public void setRead(String studentId, String articleId, boolean read) {
        String normalizedStudentId = requireStudent(studentId);
        articles.requireArticle(articleId);
        Instant now = Instant.now();
        CurrentAffairsUserState current = states.findByStudentIdAndArticleId(normalizedStudentId, articleId)
                .orElseGet(() -> CurrentAffairsUserState.builder()
                        .studentId(normalizedStudentId)
                        .articleId(articleId)
                        .build());
        persistOrDelete(current.toBuilder()
                .read(read)
                .readAt(read ? now : null)
                .build());
    }

    private void persistOrDelete(CurrentAffairsUserState state) {
        if (!state.isBookmarked() && !state.isRead()) {
            if (state.getId() != null) states.delete(state);
            return;
        }
        try {
            states.save(state);
        } catch (DuplicateKeyException exception) {
            CurrentAffairsUserState persisted = states.findByStudentIdAndArticleId(
                            state.getStudentId(), state.getArticleId())
                    .orElseThrow(() -> exception);
            states.save(persisted.toBuilder()
                    .bookmarked(state.isBookmarked())
                    .bookmarkedAt(state.getBookmarkedAt())
                    .read(state.isRead())
                    .readAt(state.getReadAt())
                    .build());
        }
    }

    private static Set<String> intersect(Set<String> current, Set<String> next) {
        if (current == null) return next;
        Set<String> intersection = new HashSet<>(current);
        intersection.retainAll(next);
        return Set.copyOf(intersection);
    }

    public record ArticleSelection(
            Set<String> includedArticleIds,
            Set<String> excludedArticleIds,
            boolean personalized
    ) {
        static ArticleSelection shared() {
            return new ArticleSelection(null, Set.of(), false);
        }
    }
}
