package com.spark.campusday.meal.absence.controller;

import com.spark.campusday.meal.absence.dto.AbsenceNoticeRequest;
import com.spark.campusday.meal.absence.dto.AbsenceNoticeResponse;
import com.spark.campusday.meal.absence.service.AbsenceNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Unsecured development API. Security will supply Student identity in a later phase. */
@RestController
@RequestMapping("/api/meal/absences")
@RequiredArgsConstructor
public class AbsenceNoticeController {
    private final AbsenceNoticeService service;

    @PostMapping
    public ResponseEntity<AbsenceNoticeResponse> create(@RequestParam String studentId,
            @Valid @RequestBody AbsenceNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(studentId, request));
    }

    @GetMapping
    public List<AbsenceNoticeResponse> list(@RequestParam String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(studentId, from, to);
    }

    @GetMapping("/{id}")
    public AbsenceNoticeResponse byId(@RequestParam String studentId, @PathVariable String id) {
        return service.getById(studentId, id);
    }
}
