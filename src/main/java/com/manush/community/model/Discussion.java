package com.manush.community.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "discussions")
public class Discussion {

    @Id
    private String id;

    private String title;
    private String content;
    private DiscussionCategory category;

    @Indexed
    private String authorId;
    private String authorName;

    private boolean pinned;
    private boolean locked;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private int viewCount;
    private int likeCount;
    private int replyCount;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum DiscussionCategory {
        GENERAL, ANNOUNCEMENTS, EVENTS, FEEDBACK, HELP_NEEDED, SUCCESS_STORIES
    }
}
