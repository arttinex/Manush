package com.manush.job.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "job_applications")
public class JobApplication {
    @Id private String id;
    private String jobId;
    private String jobTitle;
    private String applicantId;
    private String applicantName;
    private String applicantEmail;
    private String coverNote;
    private ApplicationStatus status;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public enum ApplicationStatus { APPLIED, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN }
}
