package com.manush.points.repository;

import com.manush.points.model.Badge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeRepository extends MongoRepository<Badge, String> {
    List<Badge> findByUserId(String userId);
    boolean existsByUserIdAndType(String userId, Badge.BadgeType type);
}
