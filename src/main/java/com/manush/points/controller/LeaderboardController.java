package com.manush.points.controller;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.points.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Leaderboard", description = "Community rankings and points")
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final UserRepository userRepository;

    @Operation(summary = "Get leaderboard by category and period")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard(
            @RequestParam(defaultValue = "HELPERS") String category,
            @RequestParam(defaultValue = "WEEKLY") String period,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(category, period, limit));
    }

    @Operation(summary = "Get current user's rank")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyRank(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "HELPERS") String category,
            @RequestParam(defaultValue = "WEEKLY") String period) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(leaderboardService.getUserRank(user.getId(), category, period));
    }

    @Operation(summary = "Get top 3 contributors")
    @GetMapping("/top-three")
    public ResponseEntity<List<Map<String, Object>>> getTopThree(
            @RequestParam(defaultValue = "HELPERS") String category,
            @RequestParam(defaultValue = "WEEKLY") String period) {
        return ResponseEntity.ok(leaderboardService.getTopThree(category, period));
    }

    @Operation(summary = "Get user points breakdown")
    @GetMapping("/points/{userId}")
    public ResponseEntity<Map<String, Object>> getUserPoints(@PathVariable String userId) {
        return ResponseEntity.ok(leaderboardService.getUserPointsBreakdown(userId));
    }
}
