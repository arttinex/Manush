package com.manush.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String identifier;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits")
    private String code;

    private String purpose; // EMAIL_VERIFY | PHONE_VERIFY | PASSWORD_RESET
}
