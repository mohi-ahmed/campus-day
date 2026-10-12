package com.spark.campusday.careerprep.question.repository;

import com.spark.campusday.careerprep.question.model.QuestionImportBatch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface QuestionImportBatchRepository extends MongoRepository<QuestionImportBatch, String> {
    Optional<QuestionImportBatch> findByBatchKey(String batchKey);
}
