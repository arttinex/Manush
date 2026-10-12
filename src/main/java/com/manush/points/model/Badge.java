package com.manush.points.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "badges")
public class Badge {
    @Id private String id;
    private String userId;
    private BadgeType type;
    private Instant awardedAt;

    public enum BadgeType {
        FIRST_HELPER, COMMUNITY_HELPER, BLOOD_HERO,
        TRUSTED_PROFESSIONAL, TOP_CONTRIBUTOR, EMERGENCY_HELPER
    }
}
