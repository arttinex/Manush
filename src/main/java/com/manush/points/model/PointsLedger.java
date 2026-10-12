package com.manush.points.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "points_ledger")
public class PointsLedger {
    @Id private String id;
    private String userId;
    private String reason;
    private long points;
    private String sourceId;     // helpRequestId, jobId, bloodRequestId etc.
    private PointReason pointReason;
    @CreatedDate private Instant createdAt;

    public enum PointReason {
        HELP_COMPLETED, BLOOD_DONATED, PROFESSIONAL_SERVICE,
        USEFUL_REPORT, JOB_COMPLETED, BADGE_AWARDED
    }
}
