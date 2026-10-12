package com.manush.jogajog.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.jogajog.model.Conversation;
import com.manush.jogajog.model.Message;
import com.manush.jogajog.repository.ConversationRepository;
import com.manush.jogajog.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JogajogService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public List<Conversation> getConversations(User user) {
        return conversationRepository.findByParticipantIdsContaining(user.getId());
    }

    public List<Conversation> searchConversations(User user, String query) {
        return conversationRepository.findByParticipantIdsContainingAndParticipantNamesContainingIgnoreCase(
            user.getId(), query);
    }

    public Conversation getConversation(String id) {
        return conversationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    public Conversation createConversation(User user, String participantId) {
        User participant = userRepository.findById(participantId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if conversation already exists
        List<Conversation> existing = conversationRepository.findByParticipantIdsContaining(user.getId());
        for (Conversation conv : existing) {
            if (conv.getParticipantIds() != null && conv.getParticipantIds().contains(participantId)) {
                return conv;
            }
        }
        
        Conversation conversation = new Conversation();
        List<String> ids = new ArrayList<>();
        ids.add(user.getId());
        ids.add(participantId);
        conversation.setParticipantIds(ids);

        String name1 = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "User";
        String name2 = (participant.getName() != null && !participant.getName().isBlank()) ? participant.getName() : "User";
        List<String> names = new ArrayList<>();
        names.add(name1);
        names.add(name2);
        conversation.setParticipantNames(names);
        conversation.setLastMessageTime(Instant.now());
        conversation.setCreatedAt(Instant.now());
        
        return conversationRepository.save(conversation);
    }

    public Page<Message> getMessages(String id, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(id, pageable);
    }

    public Message sendMessage(String id, User user, String text) {
        Conversation conversation = conversationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        if (conversation.getParticipantIds() != null && !conversation.getParticipantIds().contains(user.getId())) {
            throw new RuntimeException("Not a participant in this conversation");
        }
        
        Message message = new Message();
        message.setConversationId(id);
        message.setSenderId(user.getId());
        String senderName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "User";
        message.setSenderName(senderName);
        message.setText(text);
        message.setType(Message.MessageType.TEXT);
        message.setCreatedAt(Instant.now());
        
        // Update conversation last message time
        conversation.setLastMessageTime(Instant.now());
        conversation.setLastMessage(text);
        conversationRepository.save(conversation);
        
        return messageRepository.save(message);
    }

    public void blockConversation(String id, User user) {
        Conversation conversation = conversationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.setBlocked(true);
        conversation.setBlockedBy(user.getId());
        conversationRepository.save(conversation);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return userRepository.findAll().stream().limit(20).toList();
        }
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query.trim(), query.trim());
    }
}
