package com.spark.campusday.currentaffairs.repository;

import com.spark.campusday.currentaffairs.model.CurrentAffairsUserState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurrentAffairsUserStateRepository
        extends MongoRepository<CurrentAffairsUserState, String> {
    Optional<CurrentAffairsUserState> findByStudentIdAndArticleId(String studentId, String articleId);

    List<CurrentAffairsUserState> findAllByStudentIdAndArticleIdIn(
            String studentId,
            Collection<String> articleIds);

    List<CurrentAffairsUserState> findAllByStudentIdAndBookmarkedTrue(String studentId);

    List<CurrentAffairsUserState> findAllByStudentIdAndReadTrue(String studentId);
}
