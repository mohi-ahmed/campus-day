package com.spark.campusday.marketplace.image.model;

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
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_images")
@CompoundIndexes({
        @CompoundIndex(name = "marketplace_image_owner_status_created", def = "{'uploaderStudentId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "marketplace_image_listing_order", def = "{'listingId': 1, 'sortOrder': 1}")
})
public class MarketplaceImage {
    @Id
    private String id;

    @Indexed
    private String uploaderStudentId;

    @Indexed(unique = true)
    private String assetId;

    @Indexed(unique = true)
    private String publicId;

    private String secureUrl;
    private String format;
    private int width;
    private int height;
    private long bytes;
    private String originalFilename;

    @Indexed
    private String listingId;

    private int sortOrder;

    @Builder.Default
    private MarketplaceImageStatus status = MarketplaceImageStatus.TEMPORARY;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
