package com.manush.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manush.common.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true, sparse = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String phone;

    @JsonIgnore
    private String passwordHash;

    /** Set when user registered via Google OAuth */
    private String googleId;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    private String bloodGroup;   // mandatory after profile completion
    private String area;
    private String city;
    private Location location;   // 2dsphere GeoJSON point

    private String profileImageUrl;
    private String profileImageFileId;

    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean profileComplete;

    private boolean active;      // false = soft-banned
    private boolean banned;
    private String banReason;

    private long helpingPoints;

    @Builder.Default
    private Set<String> badges = new HashSet<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Inner GeoJSON location
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        @Builder.Default
        private String type = "Point";
        private double[] coordinates; // [lng, lat]
        private String area;
        private String city;
    }
}
