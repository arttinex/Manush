package com.manush.blood.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "blood_donors")
public class BloodDonor {
    @Id private String id;
    private String userId;
    private String donorName;
    private String bloodGroup;
    private boolean available;
    private Instant lastDonationDate;
    private DonationStatus donationStatus;
    private Location location;
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
    public enum DonationStatus { AVAILABLE, DONATED_RECENTLY, NOT_AVAILABLE }
}
