package com.manush.ai.service;

import com.manush.ai.model.AiUsageLog;
import com.manush.ai.provider.AiProvider;
import com.manush.ai.repository.AiUsageLogRepository;
import com.manush.auth.model.User;
import com.manush.help.model.HelpRequest;
import com.manush.help.repository.HelpRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManushAiService {

    private final AiProvider aiProvider;
    private final AiUsageLogRepository usageLogRepository;
    private final HelpRequestRepository helpRequestRepository;

    /**
     * Rate-limited conversational assistance.
     * Enforces principle: AI only advises and suggests; deterministic backend handles actual updates.
     */
    public Map<String, Object> chat(User user, String message) {
        AiProvider.AiResponse response = aiProvider.generate(message);

        AiUsageLog log = AiUsageLog.builder()
                .userId(user.getId())
                .action("CHAT")
                .prompt(message)
                .estimatedTokens(response.tokensUsed())
                .createdAt(Instant.now())
                .build();
        usageLogRepository.save(log);

        return Map.of(
                "reply", response.text(),
                "tokensUsed", response.tokensUsed()
        );
    }

    /**
     * Bangla / Banglish Natural Language help query interpreter.
     * e.g., "Bhai amar Uttara te ajke ekjon plumber lagbe" -> Extracts structured fields.
     */
    public AiProvider.HelpExtraction parseHelpPrompt(User user, String prompt) {
        AiProvider.HelpExtraction extraction = aiProvider.extractHelpRequest(prompt);

        AiUsageLog log = AiUsageLog.builder()
                .userId(user.getId())
                .action("EXTRACT_HELP")
                .prompt(prompt)
                .estimatedTokens(prompt.length() / 4 + 10)
                .createdAt(Instant.now())
                .build();
        usageLogRepository.save(log);

        return extraction;
    }

    /**
     * Helping Points & Contribution Guidance (Grounded in real open requests).
     */
    public Map<String, Object> getRecommendations(User user) {
        // Fetch real open requests to ground AI advice rather than hallucinating tasks
        List<HelpRequest> openRequests = helpRequestRepository.findAll(PageRequest.of(0, 5)).getContent();

        String advice = String.format(
                "You currently have %d helping points! Based on your location (%s), here are %d open community requests you could help with to reach the next badge milestone.",
                user.getHelpingPoints(),
                user.getArea() != null ? user.getArea() : "your area",
                openRequests.size()
        );

        return Map.of(
                "userPoints", user.getHelpingPoints(),
                "advice", advice,
                "suggestedRequests", openRequests
        );
    }
}
