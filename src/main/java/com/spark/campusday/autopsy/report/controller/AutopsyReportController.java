package com.spark.campusday.autopsy.report.controller;

import com.spark.campusday.autopsy.report.dto.ReportRequest;
import com.spark.campusday.autopsy.report.dto.ReportResponse;
import com.spark.campusday.autopsy.report.service.AutopsyReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/autopsy/posts/{postId}/reports")
@RequiredArgsConstructor
public class AutopsyReportController {
    private final AutopsyReportService reports;

    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.accepted().body(reports.create(postId, request));
    }
}
