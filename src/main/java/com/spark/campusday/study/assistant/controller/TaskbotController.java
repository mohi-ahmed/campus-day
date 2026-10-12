package com.spark.campusday.study.assistant.controller;

import com.spark.campusday.study.assistant.dto.TaskbotChatRequest;
import com.spark.campusday.study.assistant.dto.TaskbotChatResponse;
import com.spark.campusday.study.assistant.dto.TaskbotStatusResponse;
import com.spark.campusday.study.assistant.dto.TaskbotTaskDraft;
import com.spark.campusday.study.assistant.service.TaskbotService;
import com.spark.campusday.study.task.dto.StudyTaskResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Unsecured development API. Authentication will provide the student identity later. */
@Validated
@RestController
@RequestMapping("/api/study/assistant")
@RequiredArgsConstructor
public class TaskbotController {
    private final TaskbotService service;

    @GetMapping("/status")
    public TaskbotStatusResponse status() {
        return service.status();
    }

    @PostMapping("/chat")
    public TaskbotChatResponse chat(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody TaskbotChatRequest request) {
        return service.chat(studentId, request);
    }

    @PostMapping("/tasks/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyTaskResponse confirmTask(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody TaskbotTaskDraft draft) {
        return service.confirmTask(studentId, draft);
    }
}
