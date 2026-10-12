package com.manush.help.dto;

import com.manush.help.model.HelpPost;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PostResponse {
    private String id;
    private String userId;
    private String authorName;
    private String authorAvatarUrl;
    private String title;
    private String description;
    private HelpPost.PostCategory category;
    private HelpPost.Urgency urgency;
    private HelpPost.Location location;
    private List<String> imageUrls;
    private long reactionCount;
    private long commentCount;
    private long shareCount;
    private boolean pinned;
    private Instant createdAt;
}
