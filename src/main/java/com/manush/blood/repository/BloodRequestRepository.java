package com.manush.blood.repository;

import com.manush.blood.model.BloodRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends MongoRepository<BloodRequest, String> {
    Page<BloodRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId, Pageable pageable);
    List<BloodRequest> findByRequesterId(String requesterId);
    Page<BloodRequest> findByBloodGroupAndStatusOrderByCreatedAtDesc(
            String bloodGroup, BloodRequest.RequestStatus status, Pageable pageable);
}
