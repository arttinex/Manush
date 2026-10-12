package com.manush.ai.controller;

import com.manush.ai.provider.AiProvider;
import com.manush.ai.service.ManushAiService;
import com.manush.auth.model.User;
import com.manush.common.ratelimit.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Manush AI", description = "AI assistance for Bangla/English extraction and suggestions")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ManushAiService aiService;
    private final RateLimitService rateLimitService;

    public record ChatRequest(String message) {}
    public record HelpPromptRequest(String prompt) {}

    @Operation(summary = "Chat with Manush AI assistant")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody ChatRequest req,
            @AuthenticationPrincipal User user) {
        String rateLimitKey = user != null ? "ai:" + user.getId() : "ai:guest";
        rateLimitService.checkRateLimit(rateLimitKey, 30, 1);
        return ResponseEntity.ok(aiService.chat(user, req.message()));
    }

    @Operation(summary = "Parse natural language Bangla/English help request")
    @PostMapping("/help/analyze")
    public ResponseEntity<AiProvider.HelpExtraction> analyzeHelpPrompt(
            @RequestBody HelpPromptRequest req,
            @AuthenticationPrincipal User user) {
        String rateLimitKey = user != null ? "ai:" + user.getId() : "ai:guest";
        rateLimitService.checkRateLimit(rateLimitKey, 30, 1);
        return ResponseEntity.ok(aiService.parseHelpPrompt(user, req.prompt()));
    }

    @Operation(summary = "Get grounded helping opportunity recommendations")
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiService.getRecommendations(user));
    }
}
