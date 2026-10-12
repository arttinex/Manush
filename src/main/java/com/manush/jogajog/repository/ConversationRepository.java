package com.manush.jogajog.repository;

import com.manush.jogajog.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    @Query("{ 'participantIds': ?0 }")
    List<Conversation> findByParticipant(String userId);

    @Query("{ 'participantIds': { $all: [?0, ?1] } }")
    Optional<Conversation> findByParticipants(String userA, String userB);
    
    List<Conversation> findByParticipantIdsContaining(String userId);
    List<Conversation> findByParticipantIdsContainingAndParticipantNamesContainingIgnoreCase(String userId, String name);
}
