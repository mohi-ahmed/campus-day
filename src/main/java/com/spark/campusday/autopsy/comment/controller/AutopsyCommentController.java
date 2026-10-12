package com.spark.campusday.autopsy.comment.controller;

import com.spark.campusday.autopsy.comment.dto.CommentPage;
import com.spark.campusday.autopsy.comment.dto.CommentRequest;
import com.spark.campusday.autopsy.comment.dto.CommentResponse;
import com.spark.campusday.autopsy.comment.service.AutopsyCommentService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/autopsy/posts/{postId}/comments")
@RequiredArgsConstructor
public class AutopsyCommentController {
    private final AutopsyCommentService comments;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        CommentResponse created = comments.create(
                postId,
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.created(
                URI.create("/api/autopsy/posts/" + postId + "/comments/" + created.id()))
                .body(created);
    }

    @GetMapping
    public CommentPage list(
            @PathVariable @NotBlank String postId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return comments.list(postId, page, size);
    }
}
