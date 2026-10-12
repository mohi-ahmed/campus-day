package com.spark.campusday.lostfound.post.repository;

import com.spark.campusday.lostfound.post.model.LostFoundCategory;
import com.spark.campusday.lostfound.post.model.LostFoundPost;
import com.spark.campusday.lostfound.post.model.LostFoundPostStatus;
import com.spark.campusday.lostfound.post.model.LostFoundType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LostFoundPostSearchRepository {
    Page<LostFoundPost> search(
            LostFoundPostStatus status,
            LostFoundType type,
            LostFoundCategory category,
            String searchText,
            Pageable pageable);
}
