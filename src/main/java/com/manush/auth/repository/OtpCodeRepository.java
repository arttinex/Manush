package com.manush.auth.repository;

import com.manush.auth.model.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpCodeRepository extends MongoRepository<OtpCode, String> {
    Optional<OtpCode> findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
            String identifier, OtpCode.OtpPurpose purpose);
    void deleteAllByIdentifierAndPurpose(String identifier, OtpCode.OtpPurpose purpose);
}
