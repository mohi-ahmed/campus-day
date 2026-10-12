package com.spark.campusday.lostfound.post.repository;

import com.spark.campusday.lostfound.post.model.LostFoundPost;
import com.spark.campusday.lostfound.post.model.LostFoundPostStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LostFoundPostRepository
        extends MongoRepository<LostFoundPost, String>, LostFoundPostSearchRepository {
    Optional<LostFoundPost> findByIdAndStatus(String id, LostFoundPostStatus status);
}
