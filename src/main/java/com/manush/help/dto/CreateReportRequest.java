package com.manush.help.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "Reason is required")
    @Size(min = 5, max = 500, message = "Reason must be 5-500 characters")
    private String reason;
}
