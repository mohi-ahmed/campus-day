package com.spark.campusday.lostfound.conversation.dto;

import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundType;

import java.time.Instant;

public record LostFoundConversationResponse(
        String id,
        String postId,
        String postTitle,
        LostFoundType postType,
        LostFoundCategory postCategory,
        String otherStudentId,
        String otherStudentName,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        boolean onYourPost
) {
}
