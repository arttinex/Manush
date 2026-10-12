package com.manush.points.repository;

import com.manush.points.model.LeaderboardCache;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaderboardCacheRepository extends MongoRepository<LeaderboardCache, String> {
    Optional<LeaderboardCache> findByCategoryAndPeriod(String category, String period);
}
