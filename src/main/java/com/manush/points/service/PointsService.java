package com.manush.points.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.common.exception.ForbiddenException;
import com.manush.points.model.Badge;
import com.manush.points.model.PointsLedger;
import com.manush.points.repository.BadgeRepository;
import com.manush.points.repository.PointsLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);

    private final PointsLedgerRepository pointsLedgerRepository;
    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;

    /**
     * Awards points with idempotency and self-award protection.
     */
    public boolean awardPoints(String recipientUserId, String confirmerUserId,
                               PointsLedger.PointReason reason, String sourceId,
                               long points, String description) {

        // Anti-gaming rule 1: Never self-award
        if (recipientUserId.equals(confirmerUserId)) {
            log.warn("Anti-gaming triggered: user {} tried to self-award points.", confirmerUserId);
            throw new ForbiddenException("Points cannot be self-awarded.");
        }

        // Anti-gaming rule 2: Idempotency check
        if (pointsLedgerRepository.existsByUserIdAndSourceIdAndPointReason(recipientUserId, sourceId, reason)) {
            log.info("Points already awarded to user {} for source {} (reason: {}). Skipping.",
                    recipientUserId, sourceId, reason);
            return false;
        }

        // 1. Record in append-only points_ledger
        PointsLedger entry = PointsLedger.builder()
                .userId(recipientUserId)
                .reason(description)
                .points(points)
                .sourceId(sourceId)
                .pointReason(reason)
                .createdAt(Instant.now())
                .build();
        pointsLedgerRepository.save(entry);

        // 2. Increment user's aggregate helpingPoints cache
        userRepository.findById(recipientUserId).ifPresent(user -> {
            long newTotal = user.getHelpingPoints() + points;
            user.setHelpingPoints(newTotal);

            // Check badge eligibility
            evaluateBadgesForUser(user, newTotal);
            userRepository.save(user);
        });

        log.info("Awarded {} points to user {} for reason: {}", points, recipientUserId, reason);
        return true;
    }

    /**
     * Checks and assigns badges based on milestone criteria.
     */
    public void evaluateBadgesForUser(User user, long currentTotalPoints) {
        if (currentTotalPoints >= 20 && !badgeRepository.existsByUserIdAndType(user.getId(), Badge.BadgeType.FIRST_HELPER)) {
            grantBadge(user, Badge.BadgeType.FIRST_HELPER);
        }
        if (currentTotalPoints >= 100 && !badgeRepository.existsByUserIdAndType(user.getId(), Badge.BadgeType.COMMUNITY_HELPER)) {
            grantBadge(user, Badge.BadgeType.COMMUNITY_HELPER);
        }
        if (currentTotalPoints >= 500 && !badgeRepository.existsByUserIdAndType(user.getId(), Badge.BadgeType.TOP_CONTRIBUTOR)) {
            grantBadge(user, Badge.BadgeType.TOP_CONTRIBUTOR);
        }
    }

    public void grantBadge(User user, Badge.BadgeType type) {
        if (!badgeRepository.existsByUserIdAndType(user.getId(), type)) {
            Badge badge = Badge.builder()
                    .userId(user.getId())
                    .type(type)
                    .awardedAt(Instant.now())
                    .build();
            badgeRepository.save(badge);

            user.getBadges().add(type.name().toLowerCase().replace('_', '-'));
            log.info("Granted badge {} to user {}", type, user.getId());
        }
    }
}
