package com.manush.auth.repository;

import com.manush.auth.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteAllByUserId(String userId);
    long countByUserIdAndRevokedFalse(String userId);
}
