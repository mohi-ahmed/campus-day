package com.spark.campusday.study.calendar.controller;

import com.spark.campusday.study.calendar.dto.CalendarDayResponse;
import com.spark.campusday.study.calendar.dto.CalendarMonthResponse;
import com.spark.campusday.study.calendar.dto.CalendarRangeResponse;
import com.spark.campusday.study.calendar.service.StudyCalendarService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/study/calendar")
@RequiredArgsConstructor
public class StudyCalendarController {
    private final StudyCalendarService service;

    @GetMapping("/day/{date}")
    public CalendarDayResponse day(
            @RequestParam @NotBlank String studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDay(studentId, date);
    }

    @GetMapping("/month/{year}/{month}")
    public CalendarMonthResponse month(
            @RequestParam @NotBlank String studentId,
            @PathVariable @Min(1) @Max(9999) int year,
            @PathVariable @Min(1) @Max(12) int month) {
        return service.getMonth(studentId, year, month);
    }

    @GetMapping("/range")
    public CalendarRangeResponse range(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getRange(studentId, from, to);
    }
}
