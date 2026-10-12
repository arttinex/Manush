package com.manush.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier;  // email or phone

    @NotBlank(message = "Password is required")
    private String password;
}
