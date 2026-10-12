package com.spark.campusday.lostfound.comment.dto;

import java.time.Instant;

public record LostFoundCommentResponse(
        String id,
        String postId,
        String authorStudentId,
        String authorName,
        String content,
        Instant createdAt
) {
}
