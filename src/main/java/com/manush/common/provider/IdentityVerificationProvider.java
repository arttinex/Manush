package com.manush.common.provider;

public interface IdentityVerificationProvider {

    VerificationResult verify(VerificationRequest req);

    record VerificationRequest(
            String userId,
            String name,
            String documentType,
            String documentNumber,
            String documentFileId
    ) {}

    record VerificationResult(
            boolean verified,
            String status, // PENDING, VERIFIED, REJECTED
            String providerReference,
            String message
    ) {}
}
