package com.manush.common.provider;

public interface EmailProvider {
    void send(String to, String subject, String body);
}
