package com.spark.campusday.currentaffairs.model;

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

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "current_affairs_user_states")
@CompoundIndexes({
        @CompoundIndex(name = "current_affairs_user_article_unique",
                def = "{'studentId': 1, 'articleId': 1}", unique = true),
        @CompoundIndex(name = "current_affairs_user_bookmarks",
                def = "{'studentId': 1, 'bookmarked': 1, 'bookmarkedAt': -1}"),
        @CompoundIndex(name = "current_affairs_user_history",
                def = "{'studentId': 1, 'read': 1, 'readAt': -1}")
})
public class CurrentAffairsUserState {
    @Id
    private String id;
    private String studentId;
    private String articleId;
    private boolean bookmarked;
    private Instant bookmarkedAt;
    private boolean read;
    private Instant readAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
