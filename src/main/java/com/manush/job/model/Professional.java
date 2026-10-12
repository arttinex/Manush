package com.manush.job.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "professionals")
public class Professional {
    @Id private String id;
    private String userId;
    private String profession;
    private List<String> skills;
    private String experience;
    private String bio;
    private Location serviceArea;
    private boolean availability;
    private String workingHours;
    private double rating;
    private int completedJobs;
    private VerificationStatus verificationStatus;
    private long helpingPoints;
    private String portfolioUrl;
    private List<String> portfolioImageUrls;
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

    public enum VerificationStatus { PENDING, VERIFIED, REJECTED, EXPIRED }
}
