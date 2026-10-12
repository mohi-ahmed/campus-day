package com.spark.campusday.lostfound.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LostFoundCommentRequest(
        @NotBlank @Size(max = 64) String authorStudentId,
        @NotBlank @Size(max = 600) String content
) {
}
