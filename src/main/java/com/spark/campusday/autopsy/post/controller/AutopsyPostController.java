package com.spark.campusday.autopsy.post.controller;

import com.spark.campusday.autopsy.post.dto.PostPage;
import com.spark.campusday.autopsy.post.dto.PostRequest;
import com.spark.campusday.autopsy.post.dto.PostResponse;
import com.spark.campusday.autopsy.post.dto.ViewResponse;
import com.spark.campusday.autopsy.post.model.AutopsyCategory;
import com.spark.campusday.autopsy.post.service.AutopsyPostService;
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
@RequestMapping("/api/autopsy/posts")
@RequiredArgsConstructor
public class AutopsyPostController {
    private final AutopsyPostService posts;

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostRequest request,
            HttpServletRequest httpRequest) {
        PostResponse created = posts.create(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.created(URI.create("/api/autopsy/posts/" + created.id())).body(created);
    }

    @GetMapping
    public PostPage list(
            @RequestParam(required = false) AutopsyCategory category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return posts.list(category, page, size);
    }

    @GetMapping("/{postId}")
    public PostResponse get(@PathVariable @NotBlank String postId) {
        return posts.get(postId);
    }

    @PostMapping("/{postId}/views")
    public ViewResponse recordView(@PathVariable @NotBlank String postId) {
        return posts.recordView(postId);
    }
}
