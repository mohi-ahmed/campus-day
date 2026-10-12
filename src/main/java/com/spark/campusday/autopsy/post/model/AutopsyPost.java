package com.spark.campusday.autopsy.post.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "autopsy_posts")
@CompoundIndexes({
        @CompoundIndex(name = "autopsy_post_status_created", def = "{'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "autopsy_post_category_status_created", def = "{'category': 1, 'status': 1, 'createdAt': -1}")
})
public class AutopsyPost {
    @Id
    private String id;

    private AutopsyCategory category;
    private String toWhom;
    private String content;
    private String nickname;
    private String tag;
    @Builder.Default
    private AutopsyPostStatus status = AutopsyPostStatus.ACTIVE;

    @Builder.Default
    private long viewCount = 0;

    @Builder.Default
    private long commentCount = 0;

    @Builder.Default
    private long totalReactionCount = 0;

    @Builder.Default
    private long repostCount = 0;

    @Builder.Default
    private Map<String, Long> reactionCounts = new LinkedHashMap<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
