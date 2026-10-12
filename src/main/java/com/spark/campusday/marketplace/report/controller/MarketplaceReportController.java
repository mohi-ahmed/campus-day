package com.spark.campusday.marketplace.report.controller;

import com.spark.campusday.marketplace.report.dto.MarketplaceReportRequest;
import com.spark.campusday.marketplace.report.dto.MarketplaceReportResponse;
import com.spark.campusday.marketplace.report.service.MarketplaceReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/marketplace/reports")
@RequiredArgsConstructor
public class MarketplaceReportController {
    private final MarketplaceReportService service;

    @PostMapping
    public ResponseEntity<MarketplaceReportResponse> create(
            @Valid @RequestBody MarketplaceReportRequest request) {
        MarketplaceReportResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/marketplace/reports/" + created.id())).body(created);
    }
}
