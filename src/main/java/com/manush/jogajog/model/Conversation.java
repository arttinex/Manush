package com.manush.jogajog.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {
    @Id private String id;
    private List<String> participantIds;
    private List<String> participantNames;
    private String lastMessage;
    private Instant lastMessageTime;
    private long unreadCountA;
    private long unreadCountB;
    private boolean blocked;
    private String blockedBy;
    @CreatedDate private Instant createdAt;
}
