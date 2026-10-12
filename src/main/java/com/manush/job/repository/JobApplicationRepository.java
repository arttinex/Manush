package com.manush.job.repository;

import com.manush.job.model.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    List<JobApplication> findByJobIdOrderByCreatedAtDesc(String jobId);
    List<JobApplication> findByJobId(String jobId);
    List<JobApplication> findByApplicantIdOrderByCreatedAtDesc(String applicantId);
    List<JobApplication> findByApplicantId(String applicantId);
    boolean existsByJobIdAndApplicantId(String jobId, String applicantId);
}
