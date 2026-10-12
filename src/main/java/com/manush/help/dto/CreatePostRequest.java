package com.manush.help.dto;

import com.manush.help.model.HelpPost;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be 5-150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 3000, message = "Description must be 10-3000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private HelpPost.PostCategory category;

    private HelpPost.Urgency urgency = HelpPost.Urgency.NORMAL;

    private List<String> imageUrls;

    private Double latitude;
    private Double longitude;
    private String area;
    private String city;
}
