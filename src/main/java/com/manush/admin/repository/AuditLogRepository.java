package com.manush.admin.repository;

import com.manush.admin.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(String actorId, Pageable pageable);
    Page<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, String resourceId, Pageable pageable);
}
