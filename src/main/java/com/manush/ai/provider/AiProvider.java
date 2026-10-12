package com.manush.ai.provider;

public interface AiProvider {

    AiResponse generate(String prompt);

    HelpExtraction extractHelpRequest(String prompt);

    record AiResponse(String text, int tokensUsed) {}

    record HelpExtraction(
            String category,
            String location,
            String urgency,
            String date,
            String summary
    ) {}
}
