package com.spark.campusday.marketplace.offer.model;

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
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "marketplace_offers")
@CompoundIndexes({
        @CompoundIndex(name = "offer_listing_status_created", def = "{'listingId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "offer_seller_status_created", def = "{'sellerStudentId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "offer_buyer_status_created", def = "{'buyerStudentId': 1, 'status': 1, 'createdAt': -1}")
})
public class MarketplaceOffer {
    @Id
    private String id;
    private String listingId;
    private String listingTitle;
    private String buyerStudentId;
    private String buyerName;
    private String sellerStudentId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal offeredAmount;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal counterAmount;

    private String message;

    @Builder.Default
    private MarketplaceOfferStatus status = MarketplaceOfferStatus.PENDING;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
