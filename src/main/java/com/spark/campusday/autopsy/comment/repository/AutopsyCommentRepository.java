package com.spark.campusday.autopsy.comment.repository;

import com.spark.campusday.autopsy.comment.model.AutopsyComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AutopsyCommentRepository extends MongoRepository<AutopsyComment, String> {
    Page<AutopsyComment> findByPostId(String postId, Pageable pageable);
}
