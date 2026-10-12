package com.spark.campusday.currentaffairs.controller;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsArticleResponse;
import com.spark.campusday.currentaffairs.dto.CurrentAffairsPageResponse;
import com.spark.campusday.currentaffairs.service.CurrentAffairsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/current-affairs")
@RequiredArgsConstructor
public class CurrentAffairsController {
    private final CurrentAffairsService service;

    @GetMapping
    public CurrentAffairsPageResponse list(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String readStatus,
            @RequestParam(required = false) Boolean bookmarked,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String cursor) {
        return service.list(
                studentId, query, category, region, language, source, contentType,
                readStatus, bookmarked, resolvePage(page, cursor), size);
    }

    @GetMapping("/highlights")
    public List<CurrentAffairsArticleResponse> highlights(
            @RequestParam(required = false) String studentId) {
        return service.highlights(studentId);
    }

    @GetMapping("/bookmarks")
    public CurrentAffairsPageResponse bookmarks(
            @RequestParam @NotBlank String studentId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String cursor) {
        return service.list(
                studentId, query, category, region, language, source, contentType,
                null, true, resolvePage(page, cursor), size);
    }

    @GetMapping("/history")
    public CurrentAffairsPageResponse history(
            @RequestParam @NotBlank String studentId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String cursor) {
        return service.list(
                studentId, query, category, region, language, source, contentType,
                "read", null, resolvePage(page, cursor), size);
    }

    @GetMapping("/{articleId}")
    public CurrentAffairsArticleResponse get(
            @PathVariable @NotBlank String articleId,
            @RequestParam(required = false) String studentId) {
        return service.get(studentId, articleId);
    }

    @PostMapping("/{articleId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bookmark(
            @PathVariable @NotBlank String articleId,
            @RequestParam @NotBlank String studentId) {
        service.bookmark(studentId, articleId, true);
    }

    @DeleteMapping("/{articleId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(
            @PathVariable @NotBlank String articleId,
            @RequestParam @NotBlank String studentId) {
        service.bookmark(studentId, articleId, false);
    }

    @PostMapping("/{articleId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @PathVariable @NotBlank String articleId,
            @RequestParam @NotBlank String studentId) {
        service.markRead(studentId, articleId, true);
    }

    @DeleteMapping("/{articleId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markUnread(
            @PathVariable @NotBlank String articleId,
            @RequestParam @NotBlank String studentId) {
        service.markRead(studentId, articleId, false);
    }

    private static int resolvePage(int page, String cursor) {
        if (cursor == null || cursor.isBlank()) return page;
        try {
            int parsed = Integer.parseInt(cursor.trim());
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            throw new InvalidRequestException("Invalid current affairs cursor");
        }
    }
}
