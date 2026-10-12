package com.spark.campusday.marketplace.wanted.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_wanted_responses")
@CompoundIndexes({
        @CompoundIndex(name = "wanted_response_post_created", def = "{'wantedPostId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "wanted_response_unique_seller", def = "{'wantedPostId': 1, 'sellerStudentId': 1}", unique = true)
})
public class WantedPostResponse {
    @Id
    private String id;
    private String wantedPostId;
    private String sellerStudentId;
    private String sellerName;
    private String listingId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private String message;

    @Builder.Default
    private WantedResponseStatus status = WantedResponseStatus.SUBMITTED;

    @CreatedDate
    private Instant createdAt;
}
