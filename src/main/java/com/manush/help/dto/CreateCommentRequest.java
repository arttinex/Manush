package com.manush.help.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "Comment cannot be blank")
    @Size(min = 1, max = 1000, message = "Comment must be under 1000 characters")
    private String content;
}
