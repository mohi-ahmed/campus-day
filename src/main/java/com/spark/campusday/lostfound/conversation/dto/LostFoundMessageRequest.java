package com.spark.campusday.lostfound.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LostFoundMessageRequest(
        @NotBlank @Size(max = 64) String senderStudentId,
        @NotBlank @Size(max = 1200) String content
) {
}
