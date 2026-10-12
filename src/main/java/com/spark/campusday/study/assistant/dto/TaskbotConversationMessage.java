package com.spark.campusday.study.assistant.dto;

import com.spark.campusday.study.assistant.model.TaskbotConversationRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskbotConversationMessage(
        @NotNull TaskbotConversationRole role,
        @NotBlank @Size(max = 1000) String content
) {
}
