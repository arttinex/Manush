package com.manush.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "verification_states")
public class VerificationState {

    @Id
    private String id;

    private String userId;
    private VerificationStatus status;
    private String documentType;        // NID, TIN, etc.
    private String documentFileId;      // GridFS file id (private)
    private String adminNote;
    private String reviewedBy;

    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant expiresAt;          // VERIFIED expires after 2 years

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }
}
