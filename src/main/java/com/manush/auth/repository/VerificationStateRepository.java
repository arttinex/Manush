package com.manush.auth.repository;

import com.manush.auth.model.VerificationState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationStateRepository extends MongoRepository<VerificationState, String> {
    Optional<VerificationState> findByUserId(String userId);
    Page<VerificationState> findByStatus(VerificationState.VerificationStatus status, Pageable pageable);
    boolean existsByUserIdAndStatus(String userId, VerificationState.VerificationStatus status);
}
