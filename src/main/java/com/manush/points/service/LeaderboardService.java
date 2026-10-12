package com.manush.points.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.points.model.LeaderboardCache;
import com.manush.points.repository.LeaderboardCacheRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final UserRepository userRepository;
    private final LeaderboardCacheRepository cacheRepository;

    /**
     * Reads from LeaderboardCache when available, falls back to direct query.
     */
    public List<Map<String, Object>> getLeaderboard(String category, String period, int limit) {
        Optional<LeaderboardCache> cached = cacheRepository.findByCategoryAndPeriod(category, period);
        if (cached.isPresent() && cached.get().getEntries() != null && !cached.get().getEntries().isEmpty()) {
            return cached.get().getEntries().stream().limit(limit).collect(Collectors.toList());
        }

        // Direct compute fallback
        return computeLeaderboard(category, period, limit);
    }

    public List<Map<String, Object>> computeLeaderboard(String category, String period, int limit) {
        return userRepository.findAll(
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "helpingPoints"))
        ).stream()
            .map(user -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", user.getId());
                entry.put("name", user.getName());
                entry.put("points", user.getHelpingPoints());
                entry.put("badges", user.getBadges());
                entry.put("contributions", calculateContributions(user, category));
                return entry;
            })
            .collect(Collectors.toList());
    }

    /**
     * Scheduled aggregation job every 10 minutes (600,000 ms) as specified in architecture.
     */
    @Scheduled(fixedRate = 600000)
    public void refreshLeaderboardCaches() {
        log.info("Refreshing scheduled Leaderboard caches...");
        List<String> categories = List.of("HELPERS", "BLOOD_DONORS", "PROFESSIONALS", "CONTRIBUTORS");
        List<String> periods = List.of("WEEKLY", "MONTHLY", "ALL_TIME");

        for (String cat : categories) {
            for (String per : periods) {
                List<Map<String, Object>> data = computeLeaderboard(cat, per, 100);
                String cacheId = cat + ":" + per;
                LeaderboardCache cache = LeaderboardCache.builder()
                        .id(cacheId)
                        .category(cat)
                        .period(per)
                        .entries(data)
                        .refreshedAt(Instant.now())
                        .build();
                cacheRepository.save(cache);
            }
        }
        log.info("Leaderboard cache refresh completed.");
    }

    public Map<String, Object> getUserRank(String userId, String category, String period) {
        List<Map<String, Object>> leaderboard = getLeaderboard(category, period, 1000);
        
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).get("id") != null && leaderboard.get(i).get("id").equals(userId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("rank", i + 1);
                result.put("points", leaderboard.get(i).get("points"));
                result.put("name", leaderboard.get(i).get("name"));
                result.put("badges", leaderboard.get(i).get("badges"));
                return result;
            }
        }
        
        return Map.of("rank", -1, "points", 0);
    }

    public List<Map<String, Object>> getTopThree(String category, String period) {
        return getLeaderboard(category, period, 3);
    }

    public Map<String, Object> getUserPointsBreakdown(String userId) {
        return userRepository.findById(userId)
            .map(user -> {
                Map<String, Object> breakdown = new HashMap<>();
                breakdown.put("totalPoints", user.getHelpingPoints());
                breakdown.put("badges", user.getBadges());
                breakdown.put("completedHelps", (int) (user.getHelpingPoints() / 10));
                breakdown.put("bloodDonations", user.getBadges() != null && user.getBadges().contains("blood-donor") ? 1 : 0);
                breakdown.put("jobsCompleted", user.getBadges() != null && user.getBadges().contains("job-helper") ? 1 : 0);
                return breakdown;
            })
            .orElse(Map.of("totalPoints", 0, "badges", List.of()));
    }

    private Instant getStartDate(String period) {
        Instant now = Instant.now();
        return switch (period) {
            case "WEEKLY" -> now.minus(7, ChronoUnit.DAYS);
            case "MONTHLY" -> now.minus(30, ChronoUnit.DAYS);
            case "ALL_TIME" -> Instant.EPOCH;
            default -> now.minus(7, ChronoUnit.DAYS);
        };
    }

    private int calculateContributions(User user, String category) {
        return switch (category) {
            case "HELPERS" -> (int) (user.getHelpingPoints() / 10);
            case "BLOOD_DONORS" -> (user.getBadges() != null && user.getBadges().contains("blood-donor")) ? 1 : 0;
            case "PROFESSIONALS" -> (user.getBadges() != null && user.getBadges().contains("job-helper")) ? 1 : 0;
            case "CONTRIBUTORS" -> user.getBadges() != null ? user.getBadges().size() : 0;
            default -> (int) (user.getHelpingPoints() / 10);
        };
    }
}
