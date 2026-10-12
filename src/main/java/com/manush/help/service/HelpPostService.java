package com.manush.help.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.common.exception.ForbiddenException;
import com.manush.common.exception.ResourceNotFoundException;
import com.manush.common.ratelimit.RateLimitService;
import com.manush.help.dto.CreateCommentRequest;
import com.manush.help.dto.CreatePostRequest;
import com.manush.help.dto.CreateReportRequest;
import com.manush.help.dto.PostResponse;
import com.manush.help.model.Comment;
import com.manush.help.model.HelpPost;
import com.manush.help.model.PostReaction;
import com.manush.help.model.Report;
import com.manush.help.repository.CommentRepository;
import com.manush.help.repository.HelpPostRepository;
import com.manush.help.repository.PostReactionRepository;
import com.manush.help.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelpPostService {

    private final HelpPostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    @Cacheable(value = "feed", key = "#category != null ? #category.name() + '_' + #pageable.pageNumber : 'ALL_' + #pageable.pageNumber")
    public Page<PostResponse> getFeed(HelpPost.PostCategory category, Pageable pageable) {
        Page<HelpPost> posts = category != null
                ? postRepository.findByCategoryAndDeletedFalseOrderByCreatedAtDesc(category, pageable)
                : postRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable);

        return posts.map(this::mapToResponse);
    }

    @CacheEvict(value = "feed", allEntries = true)
    public PostResponse createPost(User author, CreatePostRequest req) {
        rateLimitService.checkPost(author.getId());

        HelpPost.Location loc = null;
        if (req.getLatitude() != null && req.getLongitude() != null) {
            loc = HelpPost.Location.builder()
                    .type("Point")
                    .coordinates(new double[]{req.getLongitude(), req.getLatitude()})
                    .area(req.getArea() != null ? req.getArea() : author.getArea())
                    .city(req.getCity() != null ? req.getCity() : author.getCity())
                    .build();
        } else if (author.getLocation() != null) {
            loc = HelpPost.Location.builder()
                    .type("Point")
                    .coordinates(author.getLocation().getCoordinates())
                    .area(author.getArea())
                    .city(author.getCity())
                    .build();
        }

        HelpPost post = HelpPost.builder()
                .userId(author.getId())
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .urgency(req.getUrgency() != null ? req.getUrgency() : HelpPost.Urgency.NORMAL)
                .status(HelpPost.PostStatus.ACTIVE)
                .location(loc)
                .imageUrls(req.getImageUrls())
                .reactionCount(0)
                .commentCount(0)
                .shareCount(0)
                .viewCount(0)
                .pinned(false)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        post = postRepository.save(post);
        log.info("New help post created: {} by user: {}", post.getId(), author.getId());
        return mapToResponse(post);
    }

    public PostResponse getPost(String id) {
        HelpPost post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("HelpPost", id));
        return mapToResponse(post);
    }

    @CacheEvict(value = "feed", allEntries = true)
    public void deletePost(String id, User currentUser) {
        HelpPost post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("HelpPost", id));

        // Ownership check or Admin
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.name().equals("ADMIN"));
        if (!post.getUserId().equals(currentUser.getId()) && !isAdmin) {
            throw new ForbiddenException("Cannot delete someone else's post");
        }

        post.setDeleted(true);
        postRepository.save(post);
    }

    public Map<String, Object> toggleReaction(String postId, User user) {
        HelpPost post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("HelpPost", postId));

        Optional<PostReaction> existing = reactionRepository.findByPostIdAndUserId(postId, user.getId());
        boolean reacted;
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            post.setReactionCount(Math.max(0, post.getReactionCount() - 1));
            reacted = false;
        } else {
            PostReaction reaction = PostReaction.builder()
                    .postId(postId)
                    .userId(user.getId())
                    .reactionType("LIKE")
                    .build();
            reactionRepository.save(reaction);
            post.setReactionCount(post.getReactionCount() + 1);
            reacted = true;
        }

        postRepository.save(post);
        return Map.of("reacted", reacted, "reactionCount", post.getReactionCount());
    }

    public Comment addComment(String postId, User user, CreateCommentRequest req) {
        rateLimitService.checkComment(user.getId());

        HelpPost post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("HelpPost", postId));

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(user.getId())
                .content(req.getContent())
                .deleted(false)
                .createdAt(Instant.now())
                .build();

        comment = commentRepository.save(comment);

        // Denormalized counter increment
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return comment;
    }

    public Page<Comment> getComments(String postId, Pageable pageable) {
        return commentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtDesc(postId, pageable);
    }

    public void reportPost(String postId, User user, CreateReportRequest req) {
        HelpPost post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("HelpPost", postId));

        if (reportRepository.existsByReporterIdAndTargetId(user.getId(), postId)) {
            return; // Already reported
        }

        Report report = Report.builder()
                .reporterId(user.getId())
                .targetId(postId)
                .targetType(Report.ReportTarget.POST)
                .reason(req.getReason())
                .status(Report.ReportStatus.REPORTED)
                .createdAt(Instant.now())
                .build();

        reportRepository.save(report);
        log.info("Report filed for post: {} by user: {}", postId, user.getId());
    }

    private PostResponse mapToResponse(HelpPost post) {
        String authorName = "Community Member";
        String avatarUrl = null;

        if (post.getUserId() != null) {
            Optional<User> u = userRepository.findById(post.getUserId());
            if (u.isPresent()) {
                if (u.get().getName() != null && !u.get().getName().isBlank()) {
                    authorName = u.get().getName();
                }
                avatarUrl = u.get().getProfileImageUrl();
            }
        }

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .authorName(authorName)
                .authorAvatarUrl(avatarUrl)
                .title(post.getTitle())
                .description(post.getDescription())
                .category(post.getCategory())
                .urgency(post.getUrgency() != null ? post.getUrgency() : HelpPost.Urgency.NORMAL)
                .location(post.getLocation())
                .imageUrls(post.getImageUrls())
                .reactionCount(post.getReactionCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .pinned(post.isPinned())
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt() : Instant.now())
                .build();
    }
}
