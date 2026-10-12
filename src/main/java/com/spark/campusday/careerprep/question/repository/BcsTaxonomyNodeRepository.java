package com.spark.campusday.careerprep.question.repository;

import com.spark.campusday.careerprep.question.model.BcsTaxonomyNode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BcsTaxonomyNodeRepository extends MongoRepository<BcsTaxonomyNode, String> {
    Optional<BcsTaxonomyNode> findByCode(String code);

    List<BcsTaxonomyNode> findByActiveTrue();

    List<BcsTaxonomyNode> findByParentCodeAndActiveTrueOrderByDisplayOrderAscCodeAsc(
            String parentCode);
}
