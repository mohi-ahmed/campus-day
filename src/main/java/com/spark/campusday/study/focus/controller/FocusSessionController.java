package com.spark.campusday.study.focus.controller;

import com.spark.campusday.study.focus.dto.FocusSessionRequest;
import com.spark.campusday.study.focus.dto.FocusSessionResponse;
import com.spark.campusday.study.focus.service.FocusSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Break timers are client-side and are not persisted as focus sessions. */
@Validated
@RestController
@RequestMapping("/api/study/focus-sessions")
@RequiredArgsConstructor
public class FocusSessionController {
    private final FocusSessionService service;

    @PostMapping
    public ResponseEntity<FocusSessionResponse> record(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody FocusSessionRequest request) {
        FocusSessionResponse created = service.record(studentId, request);
        return ResponseEntity.created(URI.create("/api/study/focus-sessions/" + created.id()))
                .body(created);
    }

    @GetMapping
    public List<FocusSessionResponse> between(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.listBetween(studentId, from, to);
    }

    @GetMapping("/by-date/{focusDate}")
    public List<FocusSessionResponse> byDate(
            @RequestParam @NotBlank String studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate focusDate) {
        return service.listForDate(studentId, focusDate);
    }

    @GetMapping("/by-task/{taskId}")
    public List<FocusSessionResponse> byTask(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String taskId) {
        return service.listForTask(studentId, taskId);
    }

    @GetMapping("/{sessionId}")
    public FocusSessionResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String sessionId) {
        return service.get(studentId, sessionId);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String sessionId) {
        service.delete(studentId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
