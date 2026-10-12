package com.manush.job.repository;

import com.manush.job.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    Page<Job> findByStatusOrderByCreatedAtDesc(Job.JobStatus status, Pageable pageable);
    Page<Job> findByCategoryAndStatusOrderByCreatedAtDesc(String category, Job.JobStatus status, Pageable pageable);
    
    Page<Job> findByCategory(Job.JobCategory category, Pageable pageable);
    Page<Job> findByEmploymentType(Job.EmploymentType employmentType, Pageable pageable);
    Page<Job> findByCategoryAndEmploymentType(Job.JobCategory category, Job.EmploymentType employmentType, Pageable pageable);
    Page<Job> findByLocationNear(Point location, Distance distance, Pageable pageable);
    Page<Job> findByCategoryAndLocationNear(Job.JobCategory category, Point location, Distance distance, Pageable pageable);
}
