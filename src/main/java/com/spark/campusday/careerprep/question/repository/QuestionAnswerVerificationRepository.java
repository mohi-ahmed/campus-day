package com.spark.campusday.careerprep.question.repository;

import com.spark.campusday.careerprep.question.model.QuestionAnswerVerification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface QuestionAnswerVerificationRepository
        extends MongoRepository<QuestionAnswerVerification, String> {
    Optional<QuestionAnswerVerification> findByQuestionId(String questionId);
}
