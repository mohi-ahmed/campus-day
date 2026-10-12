package com.spark.campusday.lostfound.conversation.repository;

import com.spark.campusday.lostfound.conversation.model.LostFoundMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LostFoundMessageRepository extends MongoRepository<LostFoundMessage, String> {
    Page<LostFoundMessage> findByConversationId(String conversationId, Pageable pageable);
}
