package com.spark.campusday.study.reminder.controller;

import com.spark.campusday.study.reminder.dto.StudyReminderInboxResponse;
import com.spark.campusday.study.reminder.dto.StudyReminderResponse;
import com.spark.campusday.study.reminder.service.StudyReminderService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/reminders")
@RequiredArgsConstructor
public class StudyReminderController {
    private final StudyReminderService service;

    @GetMapping
    public StudyReminderInboxResponse inbox(
            @RequestParam @NotBlank String studentId) {
        return service.inbox(studentId);
    }

    @GetMapping("/{reminderId}")
    public StudyReminderResponse byId(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String reminderId) {
        return service.get(studentId, reminderId);
    }

    @PutMapping("/{reminderId}/read")
    public StudyReminderResponse markRead(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String reminderId) {
        return service.markRead(studentId, reminderId);
    }

    @PutMapping("/{reminderId}/dismiss")
    public StudyReminderResponse dismiss(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String reminderId) {
        return service.dismiss(studentId, reminderId);
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> cancel(
            @RequestParam @NotBlank String studentId,
            @PathVariable @NotBlank String reminderId) {
        service.cancel(studentId, reminderId);
        return ResponseEntity.noContent().build();
    }
}
