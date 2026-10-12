package com.spark.campusday.careerprep.question.repository;

import com.spark.campusday.careerprep.question.model.BcsQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BcsQuestionRepository extends MongoRepository<BcsQuestion, String> {
    boolean existsBySourceKey(String sourceKey);

    Optional<BcsQuestion> findByQuestionId(String questionId);
}
