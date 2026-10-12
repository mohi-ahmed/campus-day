package com.spark.campusday.careerprep.knowledge.repository;

import com.spark.campusday.careerprep.knowledge.model.KnowledgeSection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KnowledgeSectionRepository extends MongoRepository<KnowledgeSection, String> {
    boolean existsBySourceIdAndOrdinal(String sourceId, int ordinal);

    List<KnowledgeSection> findBySourceId(String sourceId);
}
