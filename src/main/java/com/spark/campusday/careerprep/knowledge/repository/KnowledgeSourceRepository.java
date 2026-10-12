package com.spark.campusday.careerprep.knowledge.repository;

import com.spark.campusday.careerprep.knowledge.model.KnowledgeSource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface KnowledgeSourceRepository extends MongoRepository<KnowledgeSource, String> {
    Optional<KnowledgeSource> findBySourceId(String sourceId);
}
