package com.manush.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier;  // email or phone
}
