package com.spark.campusday.autopsy.reaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionRequest(
        @NotBlank @Size(max = 128) String sessionId
) {
}
