package com.spark.campusday.lostfound.conversation.repository;

import com.spark.campusday.lostfound.conversation.model.LostFoundConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LostFoundConversationRepository extends MongoRepository<LostFoundConversation, String> {
    Optional<LostFoundConversation> findByPostIdAndParticipantStudentId(
            String postId, String participantStudentId);

    List<LostFoundConversation> findByOwnerStudentIdOrParticipantStudentIdOrderByLastMessageAtDesc(
            String ownerStudentId, String participantStudentId);
}
