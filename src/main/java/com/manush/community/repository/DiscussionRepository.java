package com.manush.community.repository;

import com.manush.community.model.Discussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussionRepository extends MongoRepository<Discussion, String> {
    Page<Discussion> findByCategory(Discussion.DiscussionCategory category, Pageable pageable);
    Page<Discussion> findByPinnedTrueOrderByCreatedAtDesc(Pageable pageable);
    Page<Discussion> findByTitleContainingIgnoreCase(String query, Pageable pageable);
}
