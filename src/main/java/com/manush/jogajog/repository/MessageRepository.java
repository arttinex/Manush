package com.manush.jogajog.repository;

import com.manush.jogajog.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);
    Page<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId, Pageable pageable);
    long countByConversationIdAndSenderIdNotAndReadFalse(String conversationId, String senderId);
}
