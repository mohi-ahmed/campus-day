package com.spark.campusday.autopsy.reaction.dto;

import com.spark.campusday.autopsy.reaction.model.AutopsyReactionType;

import java.util.Map;

public record ReactionResponse(
        String postId,
        AutopsyReactionType selectedReaction,
        long totalReactions,
        Map<String, Long> reactions
) {
}
