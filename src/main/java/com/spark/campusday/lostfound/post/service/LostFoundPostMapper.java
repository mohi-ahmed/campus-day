package com.spark.campusday.lostfound.post.service;

import com.spark.campusday.lostfound.post.dto.LostFoundPostResponse;
import com.spark.campusday.lostfound.post.model.LostFoundPost;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LostFoundPostMapper {
    static LostFoundPostResponse response(LostFoundPost post) {
        return new LostFoundPostResponse(
                post.getId(),
                post.getOwnerStudentId(),
                post.getOwnerName(),
                post.getContact(),
                post.getType(),
                post.getCategory(),
                post.getTitle(),
                post.getDescription(),
                post.getLocation(),
                post.getOccurredOn(),
                post.getStatus(),
                post.getCommentCount(),
                post.getReturnedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
