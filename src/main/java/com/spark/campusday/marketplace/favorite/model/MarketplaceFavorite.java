package com.spark.campusday.marketplace.favorite.model;

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
@Document(collection = "marketplace_favorites")
@CompoundIndex(name = "marketplace_favorite_unique", def = "{'studentId': 1, 'listingId': 1}", unique = true)
public class MarketplaceFavorite {
    @Id
    private String id;
    private String studentId;
    private String listingId;

    @CreatedDate
    private Instant createdAt;
}
