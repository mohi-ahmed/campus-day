package com.spark.campusday.autopsy.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank @Size(max = 128) String sessionId,
        @NotBlank @Size(max = 500) String content,
        @Size(max = 48) String nickname
) {
}
