package com.spark.campusday.lostfound.conversation.model;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lost_found_conversations")
@CompoundIndexes({
        @CompoundIndex(name = "lost_found_conversation_post_participant", def = "{'postId': 1, 'participantStudentId': 1}", unique = true),
        @CompoundIndex(name = "lost_found_conversation_owner_last_message", def = "{'ownerStudentId': 1, 'lastMessageAt': -1}"),
        @CompoundIndex(name = "lost_found_conversation_participant_last_message", def = "{'participantStudentId': 1, 'lastMessageAt': -1}")
})
public class LostFoundConversation {
    @Id
    private String id;

    private String postId;
    private String ownerStudentId;
    private String ownerName;
    private String participantStudentId;
    private String participantName;
    private String lastMessage;
    private Instant lastMessageAt;

    @Builder.Default
    private long ownerUnreadCount = 0;

    @Builder.Default
    private long participantUnreadCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
