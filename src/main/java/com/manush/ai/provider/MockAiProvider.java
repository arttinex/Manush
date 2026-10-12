package com.manush.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@ConditionalOnProperty(name = "manush.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(MockAiProvider.class);

    @Override
    public AiResponse generate(String prompt) {
        log.info("Mock AI generating response for: {}", prompt);
        String reply;
        String lower = prompt.toLowerCase(Locale.ROOT);

        if (lower.contains("point") || lower.contains("helping points")) {
            reply = "You can earn helping points by completing help requests (+20 pts), donating blood (+50 pts), or providing rated professional services (+15 pts).";
        } else if (lower.contains("blood") || lower.contains("donor")) {
            reply = "You can quickly find compatible blood donors in the Blood Bank section or post an urgent blood request.";
        } else if (lower.contains("job") || lower.contains("work")) {
            reply = "Browse the Jobs tab for community and professional openings or verify your profile to apply for certified postings.";
        } else {
            reply = "Hello! I am Manush AI assistant. I can help interpret help requests in Bangla/English, recommend community opportunities, or guide you across the platform.";
        }

        return new AiResponse(reply, prompt.length() / 4 + 20);
    }

    @Override
    public HelpExtraction extractHelpRequest(String prompt) {
        log.info("Mock AI extracting natural language help request: {}", prompt);
        String lower = prompt.toLowerCase(Locale.ROOT);

        String category = "OTHER";
        if (lower.contains("plumber") || lower.contains("pipe") || lower.contains("leak")) {
            category = "PLUMBER";
        } else if (lower.contains("electric") || lower.contains("light") || lower.contains("fan") || lower.contains("current")) {
            category = "ELECTRICIAN";
        } else if (lower.contains("blood") || lower.contains("rokt") || lower.contains("rokto")) {
            category = "BLOOD_DONATION";
        } else if (lower.contains("food") || lower.contains("khabar")) {
            category = "FOOD_SHARING";
        } else if (lower.contains("emergency") || lower.contains("bipod")) {
            category = "EMERGENCY_HELP";
        }

        String location = "Dhaka";
        if (lower.contains("uttara")) location = "Uttara";
        else if (lower.contains("dhanmondi")) location = "Dhanmondi";
        else if (lower.contains("mirpur")) location = "Mirpur";
        else if (lower.contains("gulshan")) location = "Gulshan";
        else if (lower.contains("banani")) location = "Banani";
        else if (lower.contains("sylhet")) location = "Sylhet";
        else if (lower.contains("chittagong") || lower.contains("chattogram")) location = "Chattogram";

        String urgency = lower.contains("জরুরি") || lower.contains("urgent") || lower.contains("emergency") || lower.contains("ajke") || lower.contains("today") ? "HIGH" : "NORMAL";

        return new HelpExtraction(category, location, urgency, "TODAY", prompt);
    }
}
