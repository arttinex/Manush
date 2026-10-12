package com.manush.help.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "posts")
public class HelpPost {
    @Id private String id;
    private String userId;
    private String title;
    private String description;
    private PostCategory category;
    private Urgency urgency;
    private PostStatus status;
    private Location location;
    private List<String> imageUrls;
    private long reactionCount;
    private long commentCount;
    private long shareCount;
    private long viewCount;
    private boolean pinned;
    private boolean deleted;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Location {
        @Builder.Default
        private String type = "Point";
        private double[] coordinates;
        private String area;
        private String city;
    }

    public enum PostCategory {
        GENERAL_HELP, SCAM, AWARENESS, BLOOD_DONATION,
        EMERGENCY_HELP, JOB_OPPORTUNITY, LOST_AND_FOUND,
        COMMUNITY_NOTICE, FOOD_SHARING, GIVE_AWAY, OTHER
    }
    public enum Urgency { LOW, NORMAL, HIGH, EMERGENCY }
    public enum PostStatus { ACTIVE, HIDDEN, DELETED }
}
