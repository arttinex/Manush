package com.manush.community.controller;

import com.manush.auth.model.User;
import com.manush.community.model.Discussion;
import com.manush.community.repository.DiscussionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Community", description = "Community discussions and announcements")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final DiscussionRepository discussionRepository;

    @Operation(summary = "Get all discussions with optional category filter")
    @GetMapping("/discussions")
    public ResponseEntity<Page<Discussion>> getDiscussions(
            @RequestParam(required = false) Discussion.DiscussionCategory category,
            Pageable pageable) {
        if (category != null) {
            return ResponseEntity.ok(discussionRepository.findByCategory(category, pageable));
        }
        return ResponseEntity.ok(discussionRepository.findAll(pageable));
    }

    @Operation(summary = "Search discussions")
    @GetMapping("/discussions/search")
    public ResponseEntity<Page<Discussion>> searchDiscussions(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(discussionRepository.findByTitleContainingIgnoreCase(query, pageable));
    }

    @Operation(summary = "Get pinned discussions")
    @GetMapping("/discussions/pinned")
    public ResponseEntity<List<Discussion>> getPinnedDiscussions() {
        return ResponseEntity.ok(discussionRepository.findByPinnedTrueOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, 5)
        ).getContent());
    }

    @Operation(summary = "Get discussion by ID")
    @GetMapping("/discussions/{id}")
    public ResponseEntity<Discussion> getDiscussion(@PathVariable String id) {
        Discussion discussion = discussionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Discussion not found"));
        discussion.setViewCount(discussion.getViewCount() + 1);
        return ResponseEntity.ok(discussionRepository.save(discussion));
    }

    @Operation(summary = "Create new discussion")
    @PostMapping("/discussions")
    public ResponseEntity<Discussion> createDiscussion(
            @AuthenticationPrincipal User user,
            @RequestBody Discussion discussion) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        discussion.setAuthorId(user.getId());
        discussion.setAuthorName(user.getName());
        return ResponseEntity.ok(discussionRepository.save(discussion));
    }

    @Operation(summary = "Update discussion")
    @PatchMapping("/discussions/{id}")
    public ResponseEntity<Discussion> updateDiscussion(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestBody Discussion discussion) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        Discussion existing = discussionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Discussion not found"));
        
        if (!existing.getAuthorId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        discussion.setId(id);
        discussion.setAuthorId(existing.getAuthorId());
        discussion.setAuthorName(existing.getAuthorName());
        return ResponseEntity.ok(discussionRepository.save(discussion));
    }

    @Operation(summary = "Delete discussion")
    @DeleteMapping("/discussions/{id}")
    public ResponseEntity<Void> deleteDiscussion(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        Discussion discussion = discussionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Discussion not found"));
        
        if (!discussion.getAuthorId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        discussionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Like discussion")
    @PostMapping("/discussions/{id}/like")
    public ResponseEntity<Discussion> likeDiscussion(@PathVariable String id) {
        Discussion discussion = discussionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Discussion not found"));
        discussion.setLikeCount(discussion.getLikeCount() + 1);
        return ResponseEntity.ok(discussionRepository.save(discussion));
    }
}
