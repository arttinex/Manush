package com.manush.help.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "reports")
public class Report {
    @Id private String id;
    private String reporterId;
    private String targetId;    // post, comment, or user id
    private ReportTarget targetType;
    private String reason;
    private ReportStatus status;
    private String adminNote;
    private String resolvedBy;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public enum ReportTarget { POST, COMMENT, USER, CONVERSATION }
    public enum ReportStatus { UNVERIFIED, REPORTED, UNDER_REVIEW, CORROBORATED, CONFIRMED, REJECTED }
}
