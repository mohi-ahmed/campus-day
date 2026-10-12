package com.spark.campusday.marketplace.wanted.model;

import com.spark.campusday.marketplace.listing.model.MarketplaceCategory;
import com.spark.campusday.marketplace.listing.model.MarketplaceCondition;
import com.spark.campusday.marketplace.listing.model.MarketplacePickupZone;
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
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_wanted_posts")
@CompoundIndexes({
        @CompoundIndex(name = "wanted_status_created", def = "{'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "wanted_author_status_created", def = "{'authorStudentId': 1, 'status': 1, 'createdAt': -1}")
})
public class WantedPost {
    @Id
    private String id;

    @Indexed
    private String authorStudentId;
    private String authorName;

    @TextIndexed(weight = 5)
    private String title;

    @TextIndexed(weight = 2)
    private String description;

    @Indexed
    private MarketplaceCategory category;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumBudget;

    private MarketplaceCondition acceptableCondition;
    private MarketplacePickupZone pickupZone;

    @Builder.Default
    private WantedPostStatus status = WantedPostStatus.OPEN;

    @Builder.Default
    private long responseCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
