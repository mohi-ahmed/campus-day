package com.spark.campusday.lostfound.comment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lost_found_comments")
@CompoundIndex(name = "lost_found_comment_post_created", def = "{'postId': 1, 'createdAt': -1}")
public class LostFoundComment {
    @Id
    private String id;

    private String postId;
    private String authorStudentId;
    private String authorName;
    private String content;

    @CreatedDate
    private Instant createdAt;
}
