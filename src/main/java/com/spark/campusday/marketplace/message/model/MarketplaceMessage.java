package com.spark.campusday.marketplace.message.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_messages")
@CompoundIndexes({
        @CompoundIndex(name = "marketplace_message_sender_created", def = "{'senderStudentId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_message_recipient_created", def = "{'recipientStudentId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_message_listing_created", def = "{'listingId': 1, 'createdAt': 1}")
})
public class MarketplaceMessage {
    @Id
    private String id;
    private String senderStudentId;
    private String senderName;
    private String recipientStudentId;
    private String recipientName;
    private String listingId;
    private String listingTitle;
    private String body;

    @CreatedDate
    private Instant createdAt;
}
