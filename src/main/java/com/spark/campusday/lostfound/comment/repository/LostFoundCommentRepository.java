package com.spark.campusday.lostfound.comment.repository;

import com.spark.campusday.lostfound.comment.model.LostFoundComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LostFoundCommentRepository extends MongoRepository<LostFoundComment, String> {
    Page<LostFoundComment> findByPostId(String postId, Pageable pageable);
}
