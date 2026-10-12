package com.spark.campusday.autopsy.post.dto;

import com.spark.campusday.autopsy.post.model.AutopsyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 128) String sessionId,
        @NotNull AutopsyCategory category,
        @Size(max = 120) String toWhom,
        @NotBlank @Size(min = 3, max = 1000) String thoughts,
        @Size(max = 48) String nickname,
        @Size(max = 32) String tag
) {
}
