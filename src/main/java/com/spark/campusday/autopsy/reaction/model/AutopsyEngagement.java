package com.spark.campusday.autopsy.reaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "autopsy_engagements")
@CompoundIndex(name = "autopsy_engagement_post_actor", def = "{'postId': 1, 'actorHash': 1}", unique = true)
public class AutopsyEngagement {
    @Id
    private String id;

    private String postId;
    private String actorHash;
    private AutopsyReactionType reaction;

    @Builder.Default
    private boolean reposted = false;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
