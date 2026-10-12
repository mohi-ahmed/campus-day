package com.spark.campusday.marketplace.wanted.repository;

import com.spark.campusday.marketplace.wanted.model.WantedPostResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WantedPostResponseRepository extends MongoRepository<WantedPostResponse, String> {
    boolean existsByWantedPostIdAndSellerStudentId(String wantedPostId, String sellerStudentId);
    List<WantedPostResponse> findAllByWantedPostIdOrderByCreatedAtDesc(String wantedPostId);
}
