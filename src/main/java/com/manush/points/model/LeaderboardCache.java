package com.manush.points.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "leaderboard_cache")
public class LeaderboardCache {

    @Id
    private String id; // e.g. "HELPERS:WEEKLY"

    private String category;
    private String period;
    private List<Map<String, Object>> entries;
    private Instant refreshedAt;
}
