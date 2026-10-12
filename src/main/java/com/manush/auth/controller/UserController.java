package com.manush.auth.controller;

import com.manush.auth.dto.ProfileCompletionRequest;
import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User profile management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @Operation(summary = "Get current authenticated user profile")
    @GetMapping("/me")
    public ResponseEntity<User> getMe(@AuthenticationPrincipal User authUser) {
        if (authUser == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", authUser.getId()));
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Update current authenticated user profile")
    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal User authUser,
            @RequestBody UpdateProfileRequest req) {

        if (authUser == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", authUser.getId()));

        if (req != null) {
            if (req.name() != null && !req.name().isBlank()) {
                user.setName(req.name().trim());
            }
            if (req.phone() != null && !req.phone().isBlank()) {
                user.setPhone(req.phone().trim());
            }

            String targetArea = req.area();
            if ((targetArea == null || targetArea.isBlank()) && req.location() != null) {
                targetArea = req.location().area();
            }
            if (targetArea != null && !targetArea.isBlank()) {
                user.setArea(targetArea.trim());
                if (user.getLocation() != null) {
                    user.getLocation().setArea(targetArea.trim());
                } else {
                    user.setLocation(User.Location.builder()
                            .type("Point")
                            .area(targetArea.trim())
                            .build());
                }
            }

            String targetCity = req.city();
            if ((targetCity == null || targetCity.isBlank()) && req.location() != null) {
                targetCity = req.location().city();
            }
            if (targetCity != null && !targetCity.isBlank()) {
                user.setCity(targetCity.trim());
                if (user.getLocation() != null) {
                    user.getLocation().setCity(targetCity.trim());
                }
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Delete current authenticated user account")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User authUser) {
        if (authUser == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        userRepository.deleteById(authUser.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search users by name or email")
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "query", required = false) String query) {
        String searchQuery = (q != null && !q.isBlank()) ? q : query;
        if (searchQuery == null || searchQuery.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchQuery.trim(), searchQuery.trim()));
    }

    @Operation(summary = "Complete profile with blood group, area and location")
    @PutMapping("/me/complete-profile")
    public ResponseEntity<User> completeProfile(
            @AuthenticationPrincipal User authUser,
            @Valid @RequestBody ProfileCompletionRequest req) {

        if (authUser == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", authUser.getId()));

        user.setArea(req.getArea());
        user.setCity(req.getCity());
        user.setProfileComplete(true);

        if (req.getLatitude() != null && req.getLongitude() != null) {
            User.Location loc = User.Location.builder()
                    .type("Point")
                    .coordinates(new double[]{req.getLongitude(), req.getLatitude()})
                    .area(req.getArea())
                    .city(req.getCity())
                    .build();
            user.setLocation(loc);
        }

        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    public record UpdateProfileRequest(
            String name,
            String email,
            String phone,
            String area,
            String city,
            UpdateLocationRequest location
    ) {}

    public record UpdateLocationRequest(
            String area,
            String city
    ) {}
}
