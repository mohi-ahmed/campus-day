package com.spark.campusday.autopsy.post.service;

import com.spark.campusday.autopsy.post.dto.PostResponse;
import com.spark.campusday.autopsy.post.model.AutopsyPost;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class AutopsyPostMapper {
    private AutopsyPostMapper() {
    }

    static PostResponse response(AutopsyPost post) {
        return new PostResponse(
                post.getId(),
                post.getCategory(),
                post.getToWhom(),
                post.getContent(),
                post.getNickname(),
                post.getTag(),
                post.getViewCount(),
                post.getCommentCount(),
                post.getTotalReactionCount(),
                post.getRepostCount(),
                reactionCounts(post),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    static Map<String, Long> reactionCounts(AutopsyPost post) {
        Map<String, Long> counts = post.getReactionCounts();
        return counts == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }
}
