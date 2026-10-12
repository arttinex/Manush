package com.manush.common.provider;

public interface SmsProvider {
    void sendOtp(String phone, String code);
}
