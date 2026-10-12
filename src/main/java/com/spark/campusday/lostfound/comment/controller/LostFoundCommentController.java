package com.spark.campusday.lostfound.comment.controller;

import com.spark.campusday.lostfound.comment.dto.LostFoundCommentPageResponse;
import com.spark.campusday.lostfound.comment.dto.LostFoundCommentRequest;
import com.spark.campusday.lostfound.comment.dto.LostFoundCommentResponse;
import com.spark.campusday.lostfound.comment.service.LostFoundCommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/lost-found/posts/{postId}/comments")
@RequiredArgsConstructor
public class LostFoundCommentController {
    private final LostFoundCommentService service;

    @PostMapping
    public ResponseEntity<LostFoundCommentResponse> create(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody LostFoundCommentRequest request) {
        LostFoundCommentResponse created = service.create(postId, request);
        return ResponseEntity.created(URI.create(
                "/api/lost-found/posts/" + postId + "/comments/" + created.id())).body(created);
    }

    @GetMapping
    public LostFoundCommentPageResponse list(
            @PathVariable @NotBlank String postId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.list(postId, page, size);
    }
}
