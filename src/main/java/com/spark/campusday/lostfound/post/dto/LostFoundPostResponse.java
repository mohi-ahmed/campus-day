package com.spark.campusday.lostfound.post.dto;

import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundPostStatus;
import com.spark.campusday.lostfound.post.model.LostFoundType;

import java.time.Instant;
import java.time.LocalDate;

public record LostFoundPostResponse(
        String id,
        String ownerStudentId,
        String ownerName,
        String contact,
        LostFoundType type,
        LostFoundCategory category,
        String title,
        String description,
        String location,
        LocalDate occurredOn,
        LostFoundPostStatus status,
        long commentCount,
        Instant returnedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
