package com.spark.campusday.lostfound.conversation.controller;

import com.spark.campusday.lostfound.conversation.dto.LostFoundConversationResponse;
import com.spark.campusday.lostfound.conversation.dto.LostFoundMessageRequest;
import com.spark.campusday.lostfound.conversation.dto.LostFoundMessageResponse;
import com.spark.campusday.lostfound.conversation.dto.LostFoundThreadResponse;
import com.spark.campusday.lostfound.conversation.dto.LostFoundUnreadCountResponse;
import com.spark.campusday.lostfound.conversation.service.LostFoundConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Unsecured development API. Authentication will supply the student identity later. */
@Validated
@RestController
@RequestMapping("/api/lost-found")
@RequiredArgsConstructor
public class LostFoundConversationController {
    private final LostFoundConversationService service;

    @PostMapping("/posts/{postId}/messages")
    public ResponseEntity<LostFoundMessageResponse> start(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody LostFoundMessageRequest request) {
        LostFoundMessageResponse created = service.start(postId, request);
        return ResponseEntity.created(URI.create(
                "/api/lost-found/conversations/" + created.conversationId()
                        + "/messages/" + created.id())).body(created);
    }

    @GetMapping("/conversations")
    public List<LostFoundConversationResponse> inbox(
            @RequestParam @NotBlank String studentId) {
        return service.inbox(studentId);
    }

    @GetMapping("/conversations/unread-count")
    public LostFoundUnreadCountResponse unreadCount(
            @RequestParam @NotBlank String studentId) {
        return service.unreadCount(studentId);
    }

    @GetMapping("/conversations/{conversationId}")
    public LostFoundThreadResponse thread(
            @PathVariable @NotBlank String conversationId,
            @RequestParam @NotBlank String studentId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(50) int size) {
        return service.thread(conversationId, studentId, page, size);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<LostFoundMessageResponse> reply(
            @PathVariable @NotBlank String conversationId,
            @Valid @RequestBody LostFoundMessageRequest request) {
        LostFoundMessageResponse created = service.reply(conversationId, request);
        return ResponseEntity.created(URI.create(
                "/api/lost-found/conversations/" + conversationId
                        + "/messages/" + created.id())).body(created);
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public LostFoundUnreadCountResponse markRead(
            @PathVariable @NotBlank String conversationId,
            @RequestParam @NotBlank String studentId) {
        return service.markRead(conversationId, studentId);
    }
}
