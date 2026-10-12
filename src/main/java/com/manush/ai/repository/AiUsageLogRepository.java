package com.manush.ai.repository;

import com.manush.ai.model.AiUsageLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUsageLogRepository extends MongoRepository<AiUsageLog, String> {
    List<AiUsageLog> findByUserIdOrderByCreatedAtDesc(String userId);
}
