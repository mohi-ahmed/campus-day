package com.spark.campusday.study.overview.controller;

import com.spark.campusday.study.overview.dto.StudyOverviewResponse;
import com.spark.campusday.study.overview.service.StudyOverviewService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/overview")
@RequiredArgsConstructor
public class StudyOverviewController {
    private final StudyOverviewService service;

    @GetMapping("/today")
    public StudyOverviewResponse today(@RequestParam @NotBlank String studentId) {
        return service.getToday(studentId);
    }

    @GetMapping("/{date}")
    public StudyOverviewResponse byDate(
            @RequestParam @NotBlank String studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.get(studentId, date);
    }
}
