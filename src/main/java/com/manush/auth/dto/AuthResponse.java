package com.manush.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String name;
    private String email;
    private boolean profileComplete;
    private java.util.Set<String> roles;
}
