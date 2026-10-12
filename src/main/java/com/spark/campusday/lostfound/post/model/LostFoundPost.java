package com.spark.campusday.lostfound.post.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lost_found_posts")
@CompoundIndexes({
        @CompoundIndex(name = "lost_found_status_type_created", def = "{'status': 1, 'type': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "lost_found_status_category_created", def = "{'status': 1, 'category': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "lost_found_owner_status_created", def = "{'ownerStudentId': 1, 'status': 1, 'createdAt': -1}")
})
public class LostFoundPost {
    @Id
    private String id;

    @Indexed
    private String ownerStudentId;

    private String ownerName;
    private String contact;

    @Indexed
    private LostFoundType type;

    @Indexed
    private LostFoundCategory category;

    @TextIndexed(weight = 4)
    private String title;

    @TextIndexed(weight = 2)
    private String description;

    @TextIndexed
    private String location;

    private LocalDate occurredOn;

    @Builder.Default
    private LostFoundPostStatus status = LostFoundPostStatus.OPEN;

    @Builder.Default
    private long commentCount = 0;

    private Instant returnedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
