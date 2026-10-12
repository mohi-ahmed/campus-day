package com.spark.campusday.marketplace.wanted.controller;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.wanted.dto.WantedPostDto;
import com.spark.campusday.marketplace.wanted.dto.WantedPostPageResponse;
import com.spark.campusday.marketplace.wanted.dto.WantedPostRequest;
import com.spark.campusday.marketplace.wanted.dto.WantedResponseDto;
import com.spark.campusday.marketplace.wanted.dto.WantedResponseRequest;
import com.spark.campusday.marketplace.wanted.service.WantedPostService;
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
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/marketplace/wanted-posts")
@RequiredArgsConstructor
public class WantedPostController {
    private final WantedPostService service;

    @PostMapping
    public ResponseEntity<WantedPostDto> create(@Valid @RequestBody WantedPostRequest request) {
        WantedPostDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/marketplace/wanted-posts/" + created.id())).body(created);
    }

    @GetMapping
    public WantedPostPageResponse list(
            @RequestParam(required = false) MarketplaceCategory category,
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.list(category, query, page, size);
    }

    @GetMapping("/{postId}")
    public WantedPostDto byId(@PathVariable @NotBlank String postId) {
        return service.get(postId);
    }

    @PostMapping("/{postId}/responses")
    public ResponseEntity<WantedResponseDto> respond(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody WantedResponseRequest request) {
        WantedResponseDto created = service.respond(postId, request);
        return ResponseEntity.created(URI.create(
                "/api/marketplace/wanted-posts/" + postId + "/responses/" + created.id())).body(created);
    }

    @GetMapping("/{postId}/responses")
    public List<WantedResponseDto> responses(
            @PathVariable @NotBlank String postId,
            @RequestParam @NotBlank String authorStudentId) {
        return service.listResponses(postId, authorStudentId);
    }

    @PatchMapping("/{postId}/close")
    public WantedPostDto close(
            @PathVariable @NotBlank String postId,
            @RequestParam @NotBlank String authorStudentId) {
        return service.close(postId, authorStudentId);
    }
}
