package com.manush.help.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "help_requests")
public class HelpRequest {
    @Id private String id;
    private String requesterId;
    private String helperId;
    private String authorName;
    private String title;
    private String description;
    private String category;
    private HelpStatus status;
    private Urgency urgency;
    private Location location;
    private Instant requiredBy;
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

    public enum HelpStatus { REQUESTED, MATCHING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED, DISPUTED }
    public enum Urgency { LOW, NORMAL, HIGH, EMERGENCY }
}
