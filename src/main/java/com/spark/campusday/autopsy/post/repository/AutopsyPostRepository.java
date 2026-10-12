package com.spark.campusday.autopsy.post.repository;

import com.spark.campusday.autopsy.post.model.AutopsyCategory;
import com.spark.campusday.autopsy.post.model.AutopsyPost;
import com.spark.campusday.autopsy.post.model.AutopsyPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AutopsyPostRepository extends MongoRepository<AutopsyPost, String> {
    Optional<AutopsyPost> findByIdAndStatus(String id, AutopsyPostStatus status);

    Page<AutopsyPost> findByStatus(AutopsyPostStatus status, Pageable pageable);

    Page<AutopsyPost> findByCategoryAndStatus(
            AutopsyCategory category, AutopsyPostStatus status, Pageable pageable);
}
