package com.spark.campusday.lostfound.conversation.dto;

import java.util.List;

public record LostFoundThreadResponse(
        LostFoundConversationResponse conversation,
        List<LostFoundMessageResponse> messages,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
