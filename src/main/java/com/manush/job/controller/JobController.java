package com.manush.job.controller;

import com.manush.auth.model.User;
import com.manush.job.model.Job;
import com.manush.job.model.JobApplication;
import com.manush.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Jobs", description = "Job postings and applications")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "Get all jobs with optional filters")
    @GetMapping
    public ResponseEntity<Page<Job>> getJobs(
            @RequestParam(required = false) Job.JobCategory category,
            @RequestParam(required = false) Job.EmploymentType employmentType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "10") double radius,
            Pageable pageable) {
        return ResponseEntity.ok(jobService.getJobs(category, employmentType, lat, lng, radius, pageable));
    }

    @Operation(summary = "Get job by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @Operation(summary = "Create a new job posting")
    @PostMapping
    public ResponseEntity<Job> createJob(
            @AuthenticationPrincipal User user,
            @RequestBody Job job) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jobService.createJob(user, job));
    }

    @Operation(summary = "Update job")
    @PatchMapping("/{id}")
    public ResponseEntity<Job> updateJob(
            @PathVariable String id,
            @RequestBody Job job) {
        return ResponseEntity.ok(jobService.updateJob(id, job));
    }

    @Operation(summary = "Delete job")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable String id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apply for a job")
    @PostMapping("/{id}/applications")
    public ResponseEntity<JobApplication> applyForJob(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestBody JobApplication application) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jobService.applyForJob(id, user, application));
    }

    @Operation(summary = "Get applications for a job")
    @GetMapping("/{id}/applications")
    public ResponseEntity<List<JobApplication>> getApplications(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getApplications(id));
    }

    @Operation(summary = "Get my job applications")
    @GetMapping("/applications/my")
    public ResponseEntity<List<JobApplication>> getMyApplications(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jobService.getMyApplications(user));
    }

    @Operation(summary = "Update application status")
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<JobApplication> updateApplicationStatus(
            @PathVariable String id,
            @RequestParam String status) {
        try {
            return ResponseEntity.ok(jobService.updateApplicationStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
