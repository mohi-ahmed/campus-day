package com.spark.campusday.study.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TaskbotChatRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 10) List<@Valid TaskbotConversationMessage> history
) {
}
