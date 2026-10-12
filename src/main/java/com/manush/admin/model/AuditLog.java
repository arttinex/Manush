package com.manush.admin.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {
    @Id private String id;
    private String actorId;       // admin who did the action
    private String action;        // BAN_USER, VERIFY_PROFESSIONAL, RESOLVE_REPORT, etc.
    private String resourceType;  // USER, PROFESSIONAL, REPORT, POST
    private String resourceId;
    private String ipAddress;
    private String notes;
    @CreatedDate private Instant createdAt;
}
