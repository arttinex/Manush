package com.manush.job.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "jobs")
public class Job {
    @Id private String id;
    private String employerId;
    private String employerName;
    private String title;
    private String description;
    private JobCategory category;
    private GeoJsonPoint location;
    private String salaryMin;
    private String salaryMax;
    private List<String> skills;
    private EmploymentType employmentType;
    private JobStatus status;
    private boolean requiresVerification;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public enum JobCategory { TECHNOLOGY, HEALTHCARE, EDUCATION, RETAIL, HOSPITALITY, CONSTRUCTION, OTHER }
    public enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP }
    public enum JobStatus { OPEN, CLOSED, CANCELLED }
}
