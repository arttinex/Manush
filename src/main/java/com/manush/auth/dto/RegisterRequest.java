package com.manush.auth.dto;

import com.manush.common.model.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 60)
    private String name;

    // Either email or phone must be provided — validated in service layer
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(\\+880|0)[0-9]{10}$", message = "Invalid BD phone number")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
    private String password;

    @NotBlank(message = "Blood group is required")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood group")
    private String bloodGroup;

    /** Only USER or PROFESSIONAL accepted — ADMIN is rejected in service layer */
    private Role role = Role.USER;
}
