package com.spark.campusday.marketplace.message.repository;

import com.spark.campusday.marketplace.message.model.MarketplaceMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface MarketplaceMessageRepository extends MongoRepository<MarketplaceMessage, String> {
    List<MarketplaceMessage> findAllBySenderStudentIdInAndRecipientStudentIdInOrderByCreatedAtAsc(
            Collection<String> senderStudentIds,
            Collection<String> recipientStudentIds);
}
