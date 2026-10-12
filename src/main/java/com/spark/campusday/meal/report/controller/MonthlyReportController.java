package com.spark.campusday.meal.report.controller;

import com.spark.campusday.meal.report.service.MonthlyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal/reports")
@RequiredArgsConstructor
public class MonthlyReportController {
    private final MonthlyReportService service;
    @GetMapping("/monthly")
    public MonthlyReportService.Report monthly(@RequestParam int year, @RequestParam int month) {
        return service.monthly(year, month);
    }
}
