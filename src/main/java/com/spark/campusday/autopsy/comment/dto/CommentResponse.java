package com.spark.campusday.autopsy.comment.dto;

import java.time.Instant;

public record CommentResponse(
        String id,
        String postId,
        String content,
        String nickname,
        Instant createdAt
) {
}
