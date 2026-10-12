package com.manush.help.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "comments")
public class Comment {
    @Id private String id;
    private String postId;
    private String userId;
    private String content;
    private boolean deleted;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
