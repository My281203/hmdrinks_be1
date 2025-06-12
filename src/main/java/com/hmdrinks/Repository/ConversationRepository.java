package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByShipmentId(String shipmentId);
}
