package com.spark.campusday.study.insights.controller;

import com.spark.campusday.study.insights.dto.StudyInsightsResponse;
import com.spark.campusday.study.insights.service.StudyInsightsService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/insights")
@RequiredArgsConstructor
public class StudyInsightsController {
    private final StudyInsightsService service;

    @GetMapping
    public StudyInsightsResponse summary(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getSummary(studentId, from, to);
    }

    @GetMapping("/week")
    public StudyInsightsResponse week(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getWeek(studentId, date);
    }

    @GetMapping("/month")
    public StudyInsightsResponse month(
            @RequestParam @NotBlank String studentId,
            @RequestParam int year,
            @RequestParam int month) {
        return service.getMonth(studentId, year, month);
    }
}
