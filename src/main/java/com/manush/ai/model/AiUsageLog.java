package com.manush.ai.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_usage_logs")
public class AiUsageLog {

    @Id
    private String id;
    private String userId;
    private String action; // CHAT, EXTRACT_HELP, RECOMMEND_POINTS
    private String prompt;
    private int estimatedTokens;
    @CreatedDate
    private Instant createdAt;
}
