package com.manush.job.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.common.exception.ForbiddenException;
import com.manush.job.model.Job;
import com.manush.job.model.JobApplication;
import com.manush.job.model.Professional;
import com.manush.job.repository.JobApplicationRepository;
import com.manush.job.repository.JobRepository;
import com.manush.job.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;

    public Page<Job> getJobs(Job.JobCategory category, Job.EmploymentType employmentType, 
                            Double lat, Double lng, double radius, Pageable pageable) {
        if (lat != null && lng != null) {
            Point point = new Point(lng, lat);
            if (category != null) {
                return jobRepository.findByCategoryAndLocationNear(
                    category, point, new Distance(radius, Metrics.KILOMETERS), pageable
                );
            }
            return jobRepository.findByLocationNear(
                point, new Distance(radius, Metrics.KILOMETERS), pageable
            );
        }
        
        if (category != null && employmentType != null) {
            return jobRepository.findByCategoryAndEmploymentType(category, employmentType, pageable);
        }
        
        if (category != null) {
            return jobRepository.findByCategory(category, pageable);
        }
        
        if (employmentType != null) {
            return jobRepository.findByEmploymentType(employmentType, pageable);
        }
        
        return jobRepository.findAll(pageable);
    }

    public Job getJob(String id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    public Job createJob(User user, Job job) {
        job.setEmployerId(user.getId());
        job.setEmployerName(user.getName());
        if (user.getLocation() != null && user.getLocation().getCoordinates() != null && user.getLocation().getCoordinates().length >= 2) {
            job.setLocation(new GeoJsonPoint(user.getLocation().getCoordinates()[0], user.getLocation().getCoordinates()[1]));
        }
        return jobRepository.save(job);
    }

    public Job updateJob(String id, Job job) {
        job.setId(id);
        return jobRepository.save(job);
    }

    public void deleteJob(String id) {
        jobRepository.deleteById(id);
    }

    public JobApplication applyForJob(String id, User user, JobApplication application) {
        Job job = jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        // Verification Gate: if job requires verification or is formal posting
        if (job.isRequiresVerification()) {
            Professional professional = professionalRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ForbiddenException("Only verified professionals can apply for this job."));
            if (professional.getVerificationStatus() != Professional.VerificationStatus.VERIFIED) {
                throw new ForbiddenException("Your professional profile is not verified yet.");
            }
        }
        
        application.setJobId(id);
        application.setApplicantId(user.getId());
        application.setApplicantName(user.getName());
        application.setApplicantEmail(user.getEmail());
        application.setJobTitle(job.getTitle());
        
        return applicationRepository.save(application);
    }

    public List<JobApplication> getApplications(String jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    public List<JobApplication> getMyApplications(User user) {
        return applicationRepository.findByApplicantId(user.getId());
    }

    public JobApplication updateApplicationStatus(String id, String status) {
        JobApplication application = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        application.setStatus(JobApplication.ApplicationStatus.valueOf(status));
        return applicationRepository.save(application);
    }
}
