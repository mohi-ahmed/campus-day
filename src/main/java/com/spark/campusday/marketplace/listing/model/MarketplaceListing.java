package com.spark.campusday.marketplace.listing.model;

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
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_listings")
@CompoundIndexes({
        @CompoundIndex(name = "marketplace_listing_status_created", def = "{'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_listing_category_status_created", def = "{'category': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_listing_seller_status_created", def = "{'sellerStudentId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_listing_zone_status_created", def = "{'pickupZone': 1, 'status': 1, 'createdAt': -1}")
})
public class MarketplaceListing {
    @Id
    private String id;

    @Indexed
    private String sellerStudentId;

    private String sellerName;

    @TextIndexed(weight = 5)
    private String title;

    @TextIndexed(weight = 2)
    private String description;

    @Indexed
    private MarketplaceCategory category;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private MarketplaceCondition condition;
    private MarketplaceListingType listingType;
    private int quantity;
    private boolean negotiable;

    @TextIndexed
    @Indexed
    private MarketplacePickupZone pickupZone;

    @Builder.Default
    private MarketplaceListingStatus status = MarketplaceListingStatus.AVAILABLE;

    @Builder.Default
    private List<String> imageIds = List.of();

    @Builder.Default
    private long viewCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
