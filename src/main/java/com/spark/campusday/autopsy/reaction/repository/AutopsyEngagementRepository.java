package com.spark.campusday.autopsy.reaction.repository;

import com.spark.campusday.autopsy.reaction.model.AutopsyEngagement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AutopsyEngagementRepository extends MongoRepository<AutopsyEngagement, String> {
    Optional<AutopsyEngagement> findByPostIdAndActorHash(String postId, String actorHash);
}
