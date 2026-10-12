package com.spark.campusday.lostfound.conversation.dto;

import java.time.Instant;

public record LostFoundMessageResponse(
        String id,
        String conversationId,
        String senderStudentId,
        String senderName,
        String content,
        Instant createdAt
) {
}
