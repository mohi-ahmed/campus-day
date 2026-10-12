package com.spark.campusday.marketplace.wanted.repository;

import com.spark.campusday.marketplace.wanted.model.WantedPost;
import com.spark.campusday.marketplace.wanted.model.WantedPostStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WantedPostRepository extends MongoRepository<WantedPost, String>, WantedPostSearchRepository {
    Optional<WantedPost> findByIdAndStatus(String id, WantedPostStatus status);
}
