package com.manush.points.repository;

import com.manush.points.model.PointsLedger;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsLedgerRepository extends MongoRepository<PointsLedger, String> {
    List<PointsLedger> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndSourceIdAndPointReason(
            String userId, String sourceId, PointsLedger.PointReason reason);
}
