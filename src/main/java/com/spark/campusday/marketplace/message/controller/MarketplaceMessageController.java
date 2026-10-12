package com.spark.campusday.marketplace.message.controller;

import com.spark.campusday.marketplace.message.dto.MarketplaceMessageRequest;
import com.spark.campusday.marketplace.message.dto.MarketplaceMessageResponse;
import com.spark.campusday.marketplace.message.service.MarketplaceMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/marketplace/messages")
@RequiredArgsConstructor
public class MarketplaceMessageController {
    private final MarketplaceMessageService service;

    @PostMapping
    public ResponseEntity<MarketplaceMessageResponse> send(
            @Valid @RequestBody MarketplaceMessageRequest request) {
        MarketplaceMessageResponse created = service.send(request);
        return ResponseEntity.created(URI.create("/api/marketplace/messages/" + created.id())).body(created);
    }

    @GetMapping
    public List<MarketplaceMessageResponse> conversation(
            @RequestParam @NotBlank String studentId,
            @RequestParam @NotBlank String otherStudentId,
            @RequestParam(required = false) @Size(max = 64) String listingId) {
        return service.conversation(studentId, otherStudentId, listingId);
    }
}
