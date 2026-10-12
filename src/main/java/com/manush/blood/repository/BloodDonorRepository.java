package com.manush.blood.repository;

import com.manush.blood.model.BloodDonor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BloodDonorRepository extends MongoRepository<BloodDonor, String> {
    Optional<BloodDonor> findByUserId(String userId);
    boolean existsByUserId(String userId);
    
    Page<BloodDonor> findByBloodGroup(String bloodGroup, Pageable pageable);
    Page<BloodDonor> findByLocationNear(Point location, Distance distance, Pageable pageable);
    Page<BloodDonor> findByBloodGroupAndLocationNear(String bloodGroup, Point location, Distance distance, Pageable pageable);
}
