package com.spark.campusday.marketplace.favorite.controller;

import com.spark.campusday.marketplace.favorite.dto.MarketplaceFavoriteRequest;
import com.spark.campusday.marketplace.favorite.dto.MarketplaceFavoriteResponse;
import com.spark.campusday.marketplace.favorite.service.MarketplaceFavoriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/marketplace/favorites")
@RequiredArgsConstructor
public class MarketplaceFavoriteController {
    private final MarketplaceFavoriteService service;

    @GetMapping
    public MarketplaceFavoriteResponse list(@RequestParam @NotBlank String studentId) {
        return service.list(studentId);
    }

    @PostMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(
            @PathVariable @NotBlank String listingId,
            @Valid @RequestBody MarketplaceFavoriteRequest request) {
        service.save(listingId, request.studentId().trim());
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable @NotBlank String listingId,
            @RequestParam @NotBlank String studentId) {
        service.remove(listingId, studentId.trim());
    }
}
