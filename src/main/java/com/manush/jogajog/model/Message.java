package com.manush.jogajog.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "messages")
public class Message {
    @Id private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String text;
    private MessageType type;
    private String fileUrl;
    private boolean delivered;
    private boolean read;
    private Instant deliveredAt;
    private Instant readAt;
    @CreatedDate private Instant createdAt;

    public enum MessageType { TEXT, IMAGE, FILE }
}
