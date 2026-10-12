package com.manush.help.repository;

import com.manush.help.model.HelpPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpPostRepository extends MongoRepository<HelpPost, String> {
    Page<HelpPost> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
    Page<HelpPost> findByCategoryAndDeletedFalseOrderByCreatedAtDesc(
            HelpPost.PostCategory category, Pageable pageable);
    Page<HelpPost> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
