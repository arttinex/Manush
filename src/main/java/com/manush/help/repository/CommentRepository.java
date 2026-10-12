package com.manush.help.repository;

import com.manush.help.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    Page<Comment> findByPostIdAndDeletedFalseOrderByCreatedAtDesc(String postId, Pageable pageable);
}
