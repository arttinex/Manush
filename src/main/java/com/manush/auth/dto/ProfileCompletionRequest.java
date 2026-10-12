package com.manush.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileCompletionRequest {
    @NotBlank(message = "Area is required")
    private String area;

    @NotBlank(message = "City is required")
    private String city;

    private Double latitude;
    private Double longitude;

    // For PROFESSIONAL role
    private String profession;
}
