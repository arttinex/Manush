package com.manush.help.controller;

import com.manush.auth.model.User;
import com.manush.common.exception.ResourceNotFoundException;
import com.manush.help.dto.CreateCommentRequest;
import com.manush.help.dto.CreatePostRequest;
import com.manush.help.dto.CreateReportRequest;
import com.manush.help.dto.PostResponse;
import com.manush.help.model.Comment;
import com.manush.help.model.HelpPost;
import com.manush.help.service.HelpPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Posts", description = "Community feed and help posts")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class HelpPostController {

    private final HelpPostService postService;

    @Operation(summary = "Get feed with optional category filter")
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getFeed(
            @RequestParam(required = false) HelpPost.PostCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.getFeed(category, pageable));
    }

    @Operation(summary = "Get single post by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable String id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @Operation(summary = "Create a new post/help request")
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePostRequest req) {
        if (user == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return ResponseEntity.ok(postService.createPost(user, req));
    }

    @Operation(summary = "Delete post by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        postService.deletePost(id, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle reaction on post")
    @PostMapping("/{id}/react")
    public ResponseEntity<Map<String, Object>> toggleReaction(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return ResponseEntity.ok(postService.toggleReaction(id, user));
    }

    @Operation(summary = "Add comment to post")
    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCommentRequest req) {
        if (user == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return ResponseEntity.ok(postService.addComment(id, user, req));
    }

    @Operation(summary = "Get comments on post")
    @GetMapping("/{id}/comments")
    public ResponseEntity<Page<Comment>> getComments(
            @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.getComments(id, pageable));
    }

    @Operation(summary = "Report a post")
    @PostMapping("/{id}/report")
    public ResponseEntity<Void> reportPost(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateReportRequest req) {
        if (user == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        postService.reportPost(id, user, req);
        return ResponseEntity.noContent().build();
    }
}
