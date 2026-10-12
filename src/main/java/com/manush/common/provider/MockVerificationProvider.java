package com.manush.common.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "manush.verification.provider", havingValue = "mock", matchIfMissing = true)
public class MockVerificationProvider implements IdentityVerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockVerificationProvider.class);

    @Override
    public VerificationResult verify(VerificationRequest req) {
        log.info("MockVerificationProvider processing verification for userId: {}, documentType: {}",
                req.userId(), req.documentType());

        // Simulated auto-check or queue to PENDING
        String refId = "MOCK-VERIFY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new VerificationResult(
                false,
                "PENDING",
                refId,
                "Verification request submitted and pending administrative review."
        );
    }
}
