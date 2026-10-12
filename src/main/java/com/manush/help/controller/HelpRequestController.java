package com.manush.help.controller;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.help.model.HelpPost;
import com.manush.help.model.HelpRequest;
import com.manush.help.repository.HelpPostRepository;
import com.manush.help.repository.HelpRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@Tag(name = "Help Requests", description = "Help request management and matching")
@RestController
@RequestMapping({"/api/v1/help-requests", "/api/v1/help"})
@RequiredArgsConstructor
public class HelpRequestController {

    private final HelpRequestRepository requestRepository;
    private final HelpPostRepository postRepository;
    private final UserRepository userRepository;

    @Operation(summary = "Get all help requests with filters")
    @GetMapping({"", "/requests", "/nearby"})
    public ResponseEntity<Page<HelpRequest>> getRequests(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) HelpRequest.Urgency urgency,
            @RequestParam(required = false) HelpRequest.HelpStatus status,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "10") double radius,
            Pageable pageable) {

        Page<HelpRequest> page;
        if (lat != null && lng != null) {
            Point point = new Point(lng, lat);
            if (category != null && !category.isBlank()) {
                page = requestRepository.findByCategoryAndLocationNear(
                    category, point, new Distance(radius, Metrics.KILOMETERS), pageable
                );
            } else {
                page = requestRepository.findByLocationNear(
                    point, new Distance(radius, Metrics.KILOMETERS), pageable
                );
            }
        } else if (category != null && !category.isBlank() && urgency != null) {
            page = requestRepository.findByCategoryAndUrgency(category, urgency, pageable);
        } else if (category != null && !category.isBlank()) {
            page = requestRepository.findByCategory(category, pageable);
        } else if (urgency != null) {
            page = requestRepository.findByUrgency(urgency, pageable);
        } else if (status != null) {
            page = requestRepository.findByStatus(status, pageable);
        } else {
            page = requestRepository.findAll(pageable);
        }

        // Enrich author names for UI display if needed
        page.forEach(req -> {
            if (req.getAuthorName() == null || req.getAuthorName().isBlank()) {
                if (req.getRequesterId() != null) {
                    userRepository.findById(req.getRequesterId())
                            .ifPresent(u -> req.setAuthorName(u.getName()));
                }
                if (req.getAuthorName() == null) {
                    req.setAuthorName("Anonymous");
                }
            }
        });

        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get help request by ID")
    @GetMapping({"/{id}", "/requests/{id}"})
    public ResponseEntity<HelpRequest> getRequest(@PathVariable String id) {
        HelpRequest request = requestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Help request not found"));
        if (request.getAuthorName() == null && request.getRequesterId() != null) {
            userRepository.findById(request.getRequesterId())
                    .ifPresent(u -> request.setAuthorName(u.getName()));
        }
        return ResponseEntity.ok(request);
    }

    @Operation(summary = "Create new help request")
    @PostMapping({"", "/requests"})
    public ResponseEntity<HelpRequest> createRequest(
            @AuthenticationPrincipal User user,
            @RequestBody HelpRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        request.setRequesterId(user.getId());
        request.setAuthorName(user.getName() != null ? user.getName() : "Anonymous");

        if (request.getLocation() == null && user.getLocation() != null) {
            request.setLocation(new HelpRequest.Location(
                "Point", user.getLocation().getCoordinates(), user.getLocation().getArea(), user.getLocation().getCity()
            ));
        }
        request.setStatus(HelpRequest.HelpStatus.REQUESTED);
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(Instant.now());
        }
        request.setUpdatedAt(Instant.now());
        HelpRequest saved = requestRepository.save(request);

        // Also mirror as a HelpPost so it appears on the community feed
        try {
            HelpPost.PostCategory postCategory = HelpPost.PostCategory.GENERAL_HELP;
            if (request.getCategory() != null) {
                try {
                    postCategory = HelpPost.PostCategory.valueOf(request.getCategory());
                } catch (IllegalArgumentException ignored) {}
            }

            HelpPost.Urgency postUrgency = HelpPost.Urgency.NORMAL;
            if (request.getUrgency() != null) {
                try {
                    postUrgency = HelpPost.Urgency.valueOf(request.getUrgency().name());
                } catch (IllegalArgumentException ignored) {}
            }

            HelpPost.Location postLoc = null;
            if (saved.getLocation() != null) {
                postLoc = HelpPost.Location.builder()
                        .type("Point")
                        .coordinates(saved.getLocation().getCoordinates())
                        .area(saved.getLocation().getArea())
                        .city(saved.getLocation().getCity())
                        .build();
            }

            HelpPost post = HelpPost.builder()
                    .userId(user.getId())
                    .title(saved.getTitle())
                    .description(saved.getDescription())
                    .category(postCategory)
                    .urgency(postUrgency)
                    .status(HelpPost.PostStatus.ACTIVE)
                    .location(postLoc)
                    .reactionCount(0)
                    .commentCount(0)
                    .shareCount(0)
                    .viewCount(0)
                    .pinned(false)
                    .deleted(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            postRepository.save(post);
        } catch (Exception e) {
            log.warn("Failed to mirror help request as feed post: {}", e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Get my help requests")
    @GetMapping({"/my", "/requests/my"})
    public ResponseEntity<List<HelpRequest>> getMyRequests(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(requestRepository.findByRequesterId(user.getId()));
    }

    @Operation(summary = "Accept help request")
    @PostMapping({"/{id}/accept", "/requests/{id}/accept"})
    public ResponseEntity<HelpRequest> acceptRequest(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        HelpRequest request = requestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Help request not found"));

        request.setHelperId(user.getId());
        request.setStatus(HelpRequest.HelpStatus.ACCEPTED);

        // Award points to helper
        user.setHelpingPoints(user.getHelpingPoints() + 10);
        userRepository.save(user);

        return ResponseEntity.ok(requestRepository.save(request));
    }

    @Operation(summary = "Update help request status")
    @PatchMapping({"/{id}/status", "/requests/{id}/status"})
    public ResponseEntity<HelpRequest> updateStatus(
            @PathVariable String id,
            @RequestParam HelpRequest.HelpStatus status) {
        HelpRequest request = requestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Help request not found"));
        request.setStatus(status);
        return ResponseEntity.ok(requestRepository.save(request));
    }

    @Operation(summary = "Cancel help request")
    @DeleteMapping({"/{id}/requests", "/requests/{id}"})
    public ResponseEntity<Void> cancelRequest(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        HelpRequest request = requestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Help request not found"));

        if (!request.getRequesterId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        request.setStatus(HelpRequest.HelpStatus.CANCELLED);
        requestRepository.save(request);

        return ResponseEntity.noContent().build();
    }
}
