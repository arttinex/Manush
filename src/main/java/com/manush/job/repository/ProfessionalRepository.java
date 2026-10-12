package com.manush.job.repository;

import com.manush.job.model.Professional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalRepository extends MongoRepository<Professional, String> {
    Optional<Professional> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
