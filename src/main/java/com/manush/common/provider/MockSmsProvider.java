package com.manush.common.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock SMS — logs OTP to console in dev.
 * Replace with Twilio/BulkSMS via @ConditionalOnProperty.
 */
@Slf4j
@Service
public class MockSmsProvider implements SmsProvider {

    @Override
    public void sendOtp(String phone, String code) {
        log.info("[MOCK SMS] To: {} | OTP: {}", phone, code);
    }
}
