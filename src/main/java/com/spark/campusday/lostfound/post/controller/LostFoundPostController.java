package com.spark.campusday.lostfound.post.controller;

import com.spark.campusday.lostfound.post.dto.LostFoundPostPageResponse;
import com.spark.campusday.lostfound.post.dto.LostFoundPostRequest;
import com.spark.campusday.lostfound.post.dto.LostFoundPostResponse;
import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundType;
import com.spark.campusday.lostfound.post.service.LostFoundPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Unsecured development API. Authentication will supply the student identity later. */
@Validated
@RestController
@RequestMapping("/api/lost-found/posts")
@RequiredArgsConstructor
public class LostFoundPostController {
    private final LostFoundPostService service;

    @PostMapping
    public ResponseEntity<LostFoundPostResponse> create(
            @Valid @RequestBody LostFoundPostRequest request) {
        LostFoundPostResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/lost-found/posts/" + created.id())).body(created);
    }

    @GetMapping
    public LostFoundPostPageResponse list(
            @RequestParam(required = false) LostFoundType type,
            @RequestParam(required = false) LostFoundCategory category,
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.list(type, category, query, page, size);
    }

    @GetMapping("/{postId}")
    public LostFoundPostResponse byId(@PathVariable @NotBlank String postId) {
        return service.get(postId);
    }

    @PatchMapping("/{postId}/returned")
    public LostFoundPostResponse markReturned(
            @PathVariable @NotBlank String postId,
            @RequestParam @NotBlank String studentId) {
        return service.markReturned(postId, studentId);
    }
}
