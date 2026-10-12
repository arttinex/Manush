package com.manush.help.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reactions")
public class PostReaction {

    @Id
    private String id;

    private String postId;
    private String userId;
    private String reactionType; // LIKE, HEART, SUPPORT

    @CreatedDate
    private Instant createdAt;
}
