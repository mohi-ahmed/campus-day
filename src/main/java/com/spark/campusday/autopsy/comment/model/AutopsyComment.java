package com.spark.campusday.autopsy.comment.model;

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
@Document(collection = "autopsy_comments")
@CompoundIndex(name = "autopsy_comment_post_created", def = "{'postId': 1, 'createdAt': -1}")
public class AutopsyComment {
    @Id
    private String id;

    private String postId;
    private String content;
    private String nickname;

    @CreatedDate
    private Instant createdAt;
}
