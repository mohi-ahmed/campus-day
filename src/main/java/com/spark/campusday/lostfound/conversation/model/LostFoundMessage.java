package com.spark.campusday.lostfound.conversation.model;

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
@Document(collection = "lost_found_messages")
@CompoundIndex(name = "lost_found_message_conversation_created", def = "{'conversationId': 1, 'createdAt': -1}")
public class LostFoundMessage {
    @Id
    private String id;

    private String conversationId;
    private String senderStudentId;
    private String senderName;
    private String content;

    @CreatedDate
    private Instant createdAt;
}
