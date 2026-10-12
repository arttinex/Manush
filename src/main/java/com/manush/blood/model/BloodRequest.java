package com.manush.blood.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "blood_requests")
public class BloodRequest {
    @Id private String id;
    private String requesterId;
    private String requesterName;
    private String requesterPhone;
    private String bloodGroup;
    private String hospitalName;
    private String hospitalAddress;
    private Location location;
    private int requiredUnits;
    private Urgency urgency;
    private Instant requiredDate;
    private String description;
    private RequestStatus status;
    private String matchedDonorId;
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
    public enum Urgency { NORMAL, URGENT, EMERGENCY }
    public enum RequestStatus { REQUESTED, MATCHING, DONOR_CONTACTED, ACCEPTED, COMPLETED, CANCELLED }
}
