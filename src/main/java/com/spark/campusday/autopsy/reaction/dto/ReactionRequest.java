package com.spark.campusday.autopsy.reaction.dto;

import com.spark.campusday.autopsy.reaction.model.AutopsyReactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReactionRequest(
        @NotBlank @Size(max = 128) String sessionId,
        @NotNull AutopsyReactionType reaction
) {
}
