package com.spark.campusday.lostfound.post.dto;

import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LostFoundPostRequest(
        @NotBlank @Size(max = 64) String ownerStudentId,
        @NotNull LostFoundType type,
        @NotNull LostFoundCategory category,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 1200) String description,
        @NotBlank @Size(max = 240) String location,
        @NotNull LocalDate occurredOn,
        @Size(max = 160) String contact
) {
}
