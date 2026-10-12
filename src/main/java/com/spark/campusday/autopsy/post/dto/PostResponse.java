package com.spark.campusday.autopsy.post.dto;

import com.spark.campusday.autopsy.post.model.AutopsyCategory;

import java.time.Instant;
import java.util.Map;

public record PostResponse(
        String id,
        AutopsyCategory category,
        String toWhom,
        String thoughts,
        String nickname,
        String tag,
        long views,
        long comments,
        long totalReactions,
        long reposts,
        Map<String, Long> reactions,
        Instant createdAt,
        Instant updatedAt
) {
}
