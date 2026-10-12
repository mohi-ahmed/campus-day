package com.spark.campusday.careerprep.question.repository;

import com.spark.campusday.careerprep.question.model.QuestionClassificationProposal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface QuestionClassificationProposalRepository
        extends MongoRepository<QuestionClassificationProposal, String> {
    Optional<QuestionClassificationProposal> findByQuestionId(String questionId);
}
